# hazards-to-attributes
This is a mavenized utility that consumes the SIFIS-Home Hazards Ontology and produces the XACML code to include hazards as attributes in the Request and in the Policy.

## Usage
Run
```
mvn clean package
```
Then, run the jar file `hazards-to-attributes-1.0-SNAPSHOT-jar-with-dependencies` contained in the `target` directory.

```
java -jar ./target/hazards-to-attributes-1.0-SNAPSHOT-jar-with-dependencies.jar
```

The utility downloads the SIFIS-Home Hazards ontology from [here](https://www.sifis-home.eu/ontology/ontology.rdf) and extracts the individuals of class Hazard.
Then, for each hazard, it prints -on the standard output- the XACML code that can be used to populate an XACML request and an XACML policy.

If an argument is specified, e.g., 

```
java -jar ./target/hazards-to-attributes-1.0-SNAPSHOT-jar-with-dependencies.jar FireHazard
```

the utility outputs the XACML code for the given hazard only.
The output of the previous invocation is the following:

```
Ontology Version: v1.0.3


Individual: FireHazard

Request:
<Attribute AttributeId="eu:sifis-home:1.0:hazard:FireHazard" IncludeInResult="false">
  <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#integer">1..10</AttributeValue>
</Attribute>

Policy:
<Apply
  FunctionId="urn:oasis:names:tc:xacml:1.0:function:integer-greater-than">
  <Apply
    FunctionId="urn:oasis:names:tc:xacml:1.0:function:integer-one-and-only">
    <AttributeDesignator
      AttributeId="eu:sifis-home:1.0:hazard:FireHazard"
      Category="urn:oasis:names:tc:xacml:3.0:attribute-category:environment"
      DataType="http://www.w3.org/2001/XMLSchema#integer"
      MustBePresent="true">
    </AttributeDesignator>
  </Apply>
  <AttributeValue
    DataType="http://www.w3.org/2001/XMLSchema#integer">1..10</AttributeValue>
</Apply>
```

As first information, the utility outputs the version of the SIFIS-Home Hazard ontology that has been used.

Then, what follows `Request:` is the attribute that should be used in an XACML request among the environment attributes.

Finally, what follows `Policy:` is the attribute that can be used in an XACML policy.
This utility uses the functions `integer-greater-than` and `integer-one-and-only`, but these can be customized as needed.

Note that the value of the attribute is set to `1..10`, which is invalid for an integer.
This is intended to suggest that the accepted values are in the range [1,10]. 
It has to be customized when creating either the XACML request or the policy.

## Attributes types
Within the ontology, some hazards are characterized by a riskScore and some are not.

The hazards with a riskScore, such as `FireHazard`, are translated to XACML attributes of type `integer`.

The hazards that do not have a riskScore are instead translated to XACML attributes of type `boolean`.
When used, these attributes should have the value `true` both in the request and in the policy.
