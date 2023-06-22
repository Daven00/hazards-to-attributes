package eu.sifishome;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

public class OntologyReader {
    public static void main(String[] args) {
        // Create an instance of OWLOntologyManager to manage the ontology
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        try {
            // Load the ontology from the provided URL
            URL ontologyURL = new URL("https://www.sifis-home.eu/ontology/ontology.rdf");
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ontologyURL.openStream());

            // Retrieve ontology version from owl:versionInfo annotation
            OWLDataFactory dataFactory = manager.getOWLDataFactory();
            OWLAnnotationProperty versionProperty = dataFactory.getOWLAnnotationProperty(
                    IRI.create("http://www.w3.org/2002/07/owl#versionInfo"));
            String ontologyVersion = "-";
            for (OWLAnnotation annotation : ontology.getAnnotations()) {
                if (annotation.getProperty().equals(versionProperty) && annotation.getValue() instanceof OWLLiteral) {
                    OWLLiteral literal = (OWLLiteral) annotation.getValue();
                    ontologyVersion = literal.getLiteral();
                    break;
                }
            }
            System.out.println("Ontology Version: " + ontologyVersion);
            System.out.println();

            // Define the hazard class, the riskScore property, and the attributeId property
            IRI hazardIRI = IRI.create("https://purl.org/sifis/hazards#Hazard");
            OWLClass hazardClass = dataFactory.getOWLClass(hazardIRI);
            IRI riskScoreIRI = IRI.create("https://purl.org/sifis/hazards#riskScore");
            OWLDataProperty riskScoreProperty = dataFactory.getOWLDataProperty(riskScoreIRI);
            IRI attributeIdIRI = IRI.create("https://purl.org/sifis/hazards#attributeId");
            OWLDataProperty attributeIdProperty = dataFactory.getOWLDataProperty(attributeIdIRI);

            // Create an instance of OWLReasoner to perform reasoning
            OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
            OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);

            Set<OWLNamedIndividual> individuals;
            if (args.length > 0 && args[0] != null && !args[0].isEmpty()) {
                // User provided a named individual as input argument
                String individualShortForm = args[0];
                String individualIRI = "https://purl.org/sifis/hazards#" + individualShortForm;
                OWLNamedIndividual requestedIndividual = dataFactory.getOWLNamedIndividual(IRI.create(individualIRI));
                individuals = Collections.singleton(requestedIndividual);
            } else {
                // Retrieve all individuals in the ontology
                individuals = ontology.getIndividualsInSignature();
            }

            // Iterate over the individuals and print the information
            for (OWLNamedIndividual individual : individuals) {
                if (reasoner.getTypes(individual, false).containsEntity(hazardClass)) {
                    // The individual is of type Hazard
                    System.out.println();
                    System.out.println("Individual: " + individual.getIRI().getShortForm());
                    System.out.println();

                    String attributeId = getAttributeId(ontology, individual, attributeIdProperty);
                    boolean hasRiskScore = hasRiskScore(ontology, individual, riskScoreProperty);

                    printAttributeForRequest(attributeId, hasRiskScore);
                    printAttributeForPolicy(attributeId, hasRiskScore);

                    System.out.println("----------------------------");
                }
            }

            // Dispose the reasoner
            reasoner.dispose();
        } catch (IOException | OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }

    public static String getAttributeId(OWLOntology ontology, OWLNamedIndividual hazard, OWLDataProperty attributeIdProperty) {
        // Get the value of the attributeId data property
        for (OWLAxiom axiom : ontology.getAxioms(hazard)) {
            if (axiom instanceof OWLDataPropertyAssertionAxiom) {
                OWLDataPropertyAssertionAxiom dataPropertyAxiom = (OWLDataPropertyAssertionAxiom) axiom;
                if (dataPropertyAxiom.getProperty().equals(attributeIdProperty)) {
                    OWLLiteral attributeIdValue = dataPropertyAxiom.getObject();
                    return attributeIdValue.getLiteral();
                }
            }
        }
        return null;
    }

    public static boolean hasRiskScore(OWLOntology ontology, OWLNamedIndividual hazard, OWLDataProperty riskScoreProperty) {
        for (OWLAxiom axiom : ontology.getAxioms(hazard)) {
            if (axiom instanceof OWLDataPropertyAssertionAxiom) {
                OWLDataPropertyAssertionAxiom dataPropertyAxiom = (OWLDataPropertyAssertionAxiom) axiom;
                if (dataPropertyAxiom.getProperty().equals(riskScoreProperty)) {
                    // The hazard has the riskScore property
                    return true;
                }
            }
        }
        return false;
    }

    public static void printAttributeForRequest(String attributeId, boolean hasRiskScore) {
        System.out.println("Request:");
        System.out.println("<Attribute AttributeId=\"" + attributeId + "\" IncludeInResult=\"false\">");

        if (hasRiskScore) {
            System.out.println("  <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#integer\">1..10</AttributeValue>");
        } else {
            System.out.println("  <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#boolean\">true</AttributeValue>");
        }
        System.out.println("</Attribute>");
        System.out.println();
    }

    public static void printAttributeForPolicy(String attributeId, boolean hasRiskScore) {
        System.out.println("Policy:");
        System.out.println("<Apply");
        if (hasRiskScore) {
            System.out.println("  FunctionId=\"urn:oasis:names:tc:xacml:1.0:function:integer-greater-than\">");
        } else {
            System.out.println("  FunctionId=\"urn:oasis:names:tc:xacml:1.0:function:boolean-equal\">");
        }
        System.out.println("  <Apply");
        if (hasRiskScore) {
            System.out.println("    FunctionId=\"urn:oasis:names:tc:xacml:1.0:function:integer-one-and-only\">");
        } else {
            System.out.println("    FunctionId=\"urn:oasis:names:tc:xacml:1.0:function:boolean-one-and-only\">");
        }
        System.out.println("    <AttributeDesignator");
        System.out.println("      AttributeId=\""+ attributeId + "\"");
        System.out.println("      Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:environment\"");
        if (hasRiskScore) {
            System.out.println("      DataType=\"http://www.w3.org/2001/XMLSchema#integer\"");
        } else {
            System.out.println("      DataType=\"http://www.w3.org/2001/XMLSchema#boolean\"");
        }
        System.out.println("      MustBePresent=\"true\">");
        System.out.println("    </AttributeDesignator>");
        System.out.println("  </Apply>");
        System.out.println("  <AttributeValue");
        if (hasRiskScore) {
            System.out.println("    DataType=\"http://www.w3.org/2001/XMLSchema#integer\">1..10</AttributeValue>");
        } else {
            System.out.println("    DataType=\"http://www.w3.org/2001/XMLSchema#boolean\">true</AttributeValue>");
        }
        System.out.println("</Apply>");
        System.out.println();
    }
}
