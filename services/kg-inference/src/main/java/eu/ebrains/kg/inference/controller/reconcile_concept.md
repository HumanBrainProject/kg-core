# Reconciliation

The instances of the EBRAINS Knowledge Graph are the result of multiple 
payloads contributing to one entity and recombined according to the here
defined reconciliation logic.

## Reconciliation logic
We currently follow the approach of "the latest wins" - this means that
the last contributed value for a specific field is the one taken for the
resulting instance. Please note that this is only taken into account for
the first hierarchy level of a payload - nested documents are seen as a 
"value" and therefore replaced fully.   
The evaluation of the latest timestamp is done on a field level - not on
the document level.

## Detection of contributing instances
To figure out which documents are contributing to a single instance, we 
combine the documents by their identifiers. This means, that documents 
which (also indirectly) share identifiers are combined together. This is
only true for a single space which has as a consequence, that different
spaces could have instances with the same identifiers.  

This is a very flexible approach but it also introduces some complex
challenges because identifiers are part of the payloads of contributing
instances and therefore can change over time and trigger recombinations /
splits. Here are the possible cases we can experience:


### Case A: Single contribution
No other document shares any identifier with the uploaded document.    
Result: One new entity based on a single document

### Case B: Contribution to existing instance without introduction of new links
The document contains identifiers which are pointing to ONE already existing instance. 
Result: Update of the existing entity based on the previous and the newly introduced contribution

### Case C: Deletion of single contribution
The document introduced in case A is removed.
Result: The existing instance has now an empty payload.

### Case D: Deletion of one of multiple contributions without exclusive linking information
A document is removed which contributes to an instance and which doesn't
contain exclusive linking information (meaning all other documents are 
still linked together even without the existence of the removed document)

### Case E: Contribution to existing instance with introduction of new links (MERGE)
Given: 
* Document A with identifier "a" -> instance A' (doc A)
* Document B with identifier "b" -> instance B' (doc B)

When:
* Document C with identifiers \["a", "b"\] 

Then:
* Creation of instance C' (consisting of doc A, doc B, doc C) including ids of instance A' and instance B'
* Removal of instance A' and instance B'

### Case F: Loss of linking identifiers (SPLIT)
This can either happen due to the removal of identifiers in an update process or by the removal of a contribution with exclusive linking information
Given: 
* Document C of case E: instance C' (doc A, doc B, doc C) including identifiers A' and B' 

When: 
* Document C is updated with identifier "a" only (removal of "b")

Then:
* We decided that the SPLIT is not suited for automatic resolution. We rather will support the user in terms of manual resolution in case of conflicts of these types. This requires that identifiers are always maintained in the merge process.



Case G: Indirect dependencies

Given: 
* Document A with identifier "a"
* Document B with identifier "b"
* Document C with identifier "c"
* Document D with identifier "a", "b" 
* Document E with identifier "b", "c"
--> Instance with identifiers "a", "b", "c" consisting of docs A, B, C, D and E

## Implementation

### Insert or update
* Lookup a potentially already inferred instance for the document id of the new payload
    * Case UPDATE_DOC: A (single!) inferred instance exists
        * Get all documents previously contributing to the already existing inferred instance
        * Collect all identifiers of the documents
        * Detect if it is a split -> (if doc A a, doc B b, doc C a&b, doc D d -> inst ABC' & inst D)
        * Detect if it is invalid split -> if contributions to inferred instances have been executed, we can't do anything
    * Case INSERT_DOC: No inferred instance exists               
        * Collect all identifiers of the current document
    * Multiple inferred instances for a document id means there is a consistency issue
        * Throw exception 

    * Lookup inferred instances by collected identifiers     
        * Case NEW_INSTANCE: No inferred instance exists in case INSERT_DOC        
        * Case DELETE_INSTANCE: No inferred instance exists in case UPDATE_DOC -> this means, the last identifier has been removed -> the identifiers list is empty
        * Case UPDATE_INSTANCE: One inferred instance exists and we're in INSERT_DOC or in UPDATE_DOC while the inferred instance is the same as the one evaluated by documentid
        * Case MERGE_INSTANCE: Multiple inferred instances exist and we're in INSERT_DOC or in UPDATE_DOC while the inferred instance evaluated by documentid is one of the results
        * Case SPLIT_INSTANCE: Any other case  
     