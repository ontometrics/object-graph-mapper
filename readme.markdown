### Object Graph Mapper ###
Same idea as an ORM, but with an OGM, we are mapping objects to a graph database.

We are using reflection to support true genericity.

Goals of this project:

* Absolute minimal imprint on the code to support persistence, use existing annotations wherever possible.
* Put in an API layer so that we can use any graph database.
* Allow for discovery, e.g. automatically evolving the schema as things change.

This first version is working with neo4j.  Pretty simple to adapt to other graph/object databases.
