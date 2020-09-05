# sparql-query-versioning
This project contains the query conversion framework. Classes inside the querybuilder and querybuilder.diff packages define the methods used to adapt a query for their respective versioning method.

Jena ARQ is required for this project to work. As such, the Maven pom.xml is included along the source code.

In order to add a new versioning alternative, create a new class that extends AbstractVersionedQueryBuilder for Single Version queries. Then extend the new class with another that implements the IQueryDiffBuilder for Version Delta queries.

In order to test a query conversion, run the QueryFileReader script as follows:
> java QueryFileReader <input_file> <base_version> <target_version> <representation> <query_type> <output_file>
  
An explanation of each parameter:
> input_file:     Filename of the input file. Full path preferred.

> base_version:   Ignored for both intervals and baseline. Either 0 for first version or n for latest version. Anything other than "n" will be interpreted as 0.

> target_version: Target version for the query. For the sample set provided, numbers between 0 and 7 are valid.

> representation: The versioning representation to be used. One from "baseline", "intervals", "absolute", or "sequential".

> query_type:     The query type. Either "full" or "new".

> output_file:    Filename of the output file.
  
For example:
> java QueryFileReader test_query 0 3 abs all test_output
