# The Query API

The Query API is one of the most powerful components of the EBRAINS Knowledge Graph when it comes to consumption of metadata.

Whilst the [instances API](instances.html) is perfect for interacting with individual entities on the graph,
many use-cases for consuming the metadata involves graph traversals, which means that the information of multiple linked instances 
is supposed to be aggregated. A good example is to extract information of a dataset - although you can already find a lot of information
about the dataset itself on the dataset instance, it's often required to resolve linked instances such as the authors (to access their names),
the specimen, the licence, etc.

This is what the Query API is: It allows to run a query which is formalized in a descriptive JSON-LD structure which is either
sent at runtime or persisted in the graph for reoccurring invocation. 

## Why do you have your own query language?
Although there are several very interesting and powerful graph query languages existing already, we've decided to go with our own 
specification for the following reasons:

- We want to be able to easily reflect on the query
  - We keep the query specification as its own, regular JSON-LD document allowing us to profit from various elements, starting from the proper support of fully qualified property names in the IRI as well as compact IRI format
  - We can easily compare the query structure against the current database structure and detect potential issues if they don't properly match
  - The query API is "yet another" JSON-LD document and can be easily persisted and linked inside the graph
  - Thanks for easy reflection mechanism, we can provide convenient tooling such as the **KG Query Builder**
- We want to be as descriptive and as simple as possible
  - For simplicity, we don't want to introduce too many functional aspects into the query language. The Query API aims to be a simple way of extracting metadata in a convenient format for post-processing in any technology. 
  We therefore try to keep the focus on simple accessor mechanisms.