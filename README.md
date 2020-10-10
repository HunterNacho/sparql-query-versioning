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


# Conversion examples
In this section we show the conversion for a simple query. All queries presented have the same target version.

## Base Query:
```
PREFIX wd: <http://www.wikidata.org/entity/>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX wikibase: <http://wikiba.se/ontology#>
SELECT ?item ?fr ?geoLocation ?image
WHERE {
    ?item wdt:P463 wd:Q1010307 ;  # member of Les Plus Beaux Villages de France (organisation)
          wdt:P625 ?geoLocation .
    OPTIONAL { ?item wdt:P18 ?image }
    OPTIONAL{?item rdfs:label ?fr . FILTER(LANG(?fr) = "fr")}
}
```

## Single Version conversions
### Independent Copies

```
SPARQL
SELECT  ?item ?fr ?geoLocation ?image
FROM <http://wikidata.org/20170830>
WHERE
  { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> ;
           <http://www.wikidata.org/prop/direct/P625>  ?geoLocation
    OPTIONAL
      { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }
    OPTIONAL
      { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr
        FILTER ( lang(?fr) = "fr" )
      }
  }
;
```

### Timestamp-based

```
SPARQL
SELECT  ?item ?fr ?geoLocation ?image
FROM <http://wikidata.org/intervals/20170809-20170830>
FROM <http://wikidata.org/intervals/20170809-20170907>
FROM <http://wikidata.org/intervals/20170809-20170913>
FROM <http://wikidata.org/intervals/20170809-20170920>
FROM <http://wikidata.org/intervals/20170809-20170927>
FROM <http://wikidata.org/intervals/20170816-20170830>
FROM <http://wikidata.org/intervals/20170816-20170907>
FROM <http://wikidata.org/intervals/20170816-20170913>
FROM <http://wikidata.org/intervals/20170816-20170920>
FROM <http://wikidata.org/intervals/20170816-20170927>
FROM <http://wikidata.org/intervals/20170823-20170830>
FROM <http://wikidata.org/intervals/20170823-20170907>
FROM <http://wikidata.org/intervals/20170823-20170913>
FROM <http://wikidata.org/intervals/20170823-20170920>
FROM <http://wikidata.org/intervals/20170823-20170927>
FROM <http://wikidata.org/intervals/20170830-20170830>
FROM <http://wikidata.org/intervals/20170830-20170907>
FROM <http://wikidata.org/intervals/20170830-20170913>
FROM <http://wikidata.org/intervals/20170830-20170920>
FROM <http://wikidata.org/intervals/20170830-20170927>
WHERE
  { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> ;
           <http://www.wikidata.org/prop/direct/P625>  ?geoLocation
    OPTIONAL
      { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }
    OPTIONAL
      { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr
        FILTER ( lang(?fr) = "fr" )
      }
  }
;
```

### Differential backups

```
SPARQL
SELECT  ?item ?fr ?geoLocation ?image
FROM <http://wikidata.org/20170809>
FROM <http://wikidata.org/deltas/20170830-20170809>
FROM NAMED <http://wikidata.org/deltas/20170809-20170830>
WHERE
  { { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> ;
             <http://www.wikidata.org/prop/direct/P625>  ?geoLocation
      MINUS
        { GRAPH <http://wikidata.org/deltas/20170809-20170830>
            { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> }}
    }
    MINUS
      { GRAPH <http://wikidata.org/deltas/20170809-20170830>
          { ?item  <http://www.wikidata.org/prop/direct/P625>  ?geoLocation }}
    OPTIONAL
      { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image
        MINUS
          { GRAPH <http://wikidata.org/deltas/20170809-20170830>
              { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }}
      }
    OPTIONAL
      { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr
        MINUS
          { GRAPH <http://wikidata.org/deltas/20170809-20170830>
              { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr }}
        FILTER ( lang(?fr) = "fr" )
      }
  }
;
```

### Incremental backups

```
SPARQL
SELECT  ?item ?fr ?geoLocation ?image
WHERE
  { { { SELECT  ?item (strdt(MAX(?v0_), <http://www.w3.org/2001/XMLSchema#string>) AS ?max_v0)
        WHERE
          { { { GRAPH ?g0
                  { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> }
                ?g0  <http://wikidata.org/deltas/direction>  "forwards"^^<http://www.w3.org/2001/XMLSchema#string> . 
                ?g0  <http://wikidata.org/deltas/version>  ?v0
              }
              BIND(str(?v0) AS ?v0_)
            }
            FILTER ( str(?v0) <= "20170830" )
          }
        GROUP BY ?item
      }
      OPTIONAL
        { { SELECT  ?item (strdt(MAX(?v1_), <http://www.w3.org/2001/XMLSchema#string>) AS ?max_v1)
            WHERE
              { { { GRAPH ?g1
                      { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> }
                    ?g1  <http://wikidata.org/deltas/direction>  "backwards"^^<http://www.w3.org/2001/XMLSchema#string> . 
                    ?g1  <http://wikidata.org/deltas/version>  ?v1
                  }
                  BIND(str(?v1) AS ?v1_)
                }
                FILTER ( str(?v1) <= "20170830" )
              }
            GROUP BY ?item
          }
          FILTER ( ( ! bound(?max_v1) ) || ( str(?max_v0) > str(?max_v1) ) )
        }
    }
    { { SELECT  ?item ?geoLocation (strdt(MAX(?v2_), <http://www.w3.org/2001/XMLSchema#string>) AS ?max_v2)
        WHERE
          { { { GRAPH ?g2
                  { ?item  <http://www.wikidata.org/prop/direct/P625>  ?geoLocation }
                ?g2  <http://wikidata.org/deltas/direction>  "forwards"^^<http://www.w3.org/2001/XMLSchema#string> . 
                ?g2  <http://wikidata.org/deltas/version>  ?v2
              }
              BIND(str(?v2) AS ?v2_)
            }
            FILTER ( str(?v2) <= "20170830" )
          }
        GROUP BY ?item ?geoLocation
      }
      OPTIONAL
        { { SELECT  ?item ?geoLocation (strdt(MAX(?v3_), <http://www.w3.org/2001/XMLSchema#string>) AS ?max_v3)
            WHERE
              { { { GRAPH ?g3
                      { ?item  <http://www.wikidata.org/prop/direct/P625>  ?geoLocation }
                    ?g3  <http://wikidata.org/deltas/direction>  "backwards"^^<http://www.w3.org/2001/XMLSchema#string> . 
                    ?g3  <http://wikidata.org/deltas/version>  ?v3
                  }
                  BIND(str(?v3) AS ?v3_)
                }
                FILTER ( str(?v3) <= "20170830" )
              }
            GROUP BY ?item ?geoLocation
          }
          FILTER ( ( ! bound(?max_v3) ) || ( str(?max_v2) > str(?max_v3) ) )
        }
    }
    OPTIONAL
      { { SELECT  ?item ?image (strdt(MAX(?v4_), <http://www.w3.org/2001/XMLSchema#string>) AS ?max_v4)
          WHERE
            { { { GRAPH ?g4
                    { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }
                  ?g4  <http://wikidata.org/deltas/direction>  "forwards"^^<http://www.w3.org/2001/XMLSchema#string> . 
                  ?g4  <http://wikidata.org/deltas/version>  ?v4
                }
                BIND(str(?v4) AS ?v4_)
              }
              FILTER ( str(?v4) <= "20170830" )
            }
          GROUP BY ?item ?image
        }
        OPTIONAL
          { { SELECT  ?item ?image (strdt(MAX(?v5_), <http://www.w3.org/2001/XMLSchema#string>) AS ?max_v5)
              WHERE
                { { { GRAPH ?g5
                        { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }
                      ?g5  <http://wikidata.org/deltas/direction>  "backwards"^^<http://www.w3.org/2001/XMLSchema#string> . 
                      ?g5  <http://wikidata.org/deltas/version>  ?v5
                    }
                    BIND(str(?v5) AS ?v5_)
                  }
                  FILTER ( str(?v5) <= "20170830" )
                }
              GROUP BY ?item ?image
            }
            FILTER ( ( ! bound(?max_v5) ) || ( str(?max_v4) > str(?max_v5) ) )
          }
      }
    OPTIONAL
      { { { SELECT  ?item ?fr (strdt(MAX(?v6_), <http://www.w3.org/2001/XMLSchema#string>) AS ?max_v6)
            WHERE
              { { { GRAPH ?g6
                      { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr }
                    ?g6  <http://wikidata.org/deltas/direction>  "forwards"^^<http://www.w3.org/2001/XMLSchema#string> . 
                    ?g6  <http://wikidata.org/deltas/version>  ?v6
                  }
                  BIND(str(?v6) AS ?v6_)
                }
                FILTER ( str(?v6) <= "20170830" )
              }
            GROUP BY ?item ?fr
            HAVING ( lang(?fr) = "en" )
          }
          OPTIONAL
            { { SELECT  ?item ?fr (strdt(MAX(?v7_), <http://www.w3.org/2001/XMLSchema#string>) AS ?max_v7)
                WHERE
                  { { { GRAPH ?g7
                          { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr }
                        ?g7  <http://wikidata.org/deltas/direction>  "backwards"^^<http://www.w3.org/2001/XMLSchema#string> . 
                        ?g7  <http://wikidata.org/deltas/version>  ?v7
                      }
                      BIND(str(?v7) AS ?v7_)
                    }
                    FILTER ( str(?v7) <= "20170830" )
                  }
                GROUP BY ?item ?fr
                HAVING ( lang(?fr) = "en" )
              }
              FILTER ( ( ! bound(?max_v7) ) || ( str(?max_v6) > str(?max_v7) ) )
            }
        }
        FILTER ( lang(?fr) = "fr" )
      }
  }
;
```

### Reverse-differential backups

```
SPARQL
SELECT  ?item ?fr ?geoLocation ?image
FROM <http://wikidata.org/20170927>
FROM <http://wikidata.org/deltas/20170830-20170927>
FROM NAMED <http://wikidata.org/deltas/20170927-20170830>
WHERE
  { { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> ;
             <http://www.wikidata.org/prop/direct/P625>  ?geoLocation
      MINUS
        { GRAPH <http://wikidata.org/deltas/20170927-20170830>
            { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> }}
    }
    MINUS
      { GRAPH <http://wikidata.org/deltas/20170927-20170830>
          { ?item  <http://www.wikidata.org/prop/direct/P625>  ?geoLocation }}
    OPTIONAL
      { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image
        MINUS
          { GRAPH <http://wikidata.org/deltas/20170927-20170830>
              { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }}
      }
    OPTIONAL
      { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr
        MINUS
          { GRAPH <http://wikidata.org/deltas/20170927-20170830>
              { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr }}
        FILTER ( lang(?fr) = "fr" )
      }
  }
;
```

### Reverse-incremental backups

```
SPARQL
SELECT  ?item ?fr ?geoLocation ?image
WHERE
  { { { SELECT  ?item (strdt(MIN(?v0_), <http://www.w3.org/2001/XMLSchema#string>) AS ?max_v0)
        WHERE
          { { { GRAPH ?g0
                  { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> }
                ?g0  <http://wikidata.org/deltas/direction>  "backwards"^^<http://www.w3.org/2001/XMLSchema#string> . 
                ?g0  <http://wikidata.org/deltas/version>  ?v0
              }
              BIND(str(?v0) AS ?v0_)
            }
            FILTER ( str(?v0) >= "20170830" )
          }
        GROUP BY ?item
      }
      OPTIONAL
        { { SELECT  ?item (strdt(MIN(?v1_), <http://www.w3.org/2001/XMLSchema#string>) AS ?max_v1)
            WHERE
              { { { GRAPH ?g1
                      { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> }
                    ?g1  <http://wikidata.org/deltas/direction>  "forwards"^^<http://www.w3.org/2001/XMLSchema#string> . 
                    ?g1  <http://wikidata.org/deltas/version>  ?v1
                  }
                  BIND(str(?v1) AS ?v1_)
                }
                FILTER ( str(?v1) >= "20170830" )
              }
            GROUP BY ?item
          }
          FILTER ( ( ! bound(?max_v1) ) || ( str(?max_v0) < str(?max_v1) ) )
        }
    }
    { { SELECT  ?item ?geoLocation (strdt(MIN(?v2_), <http://www.w3.org/2001/XMLSchema#string>) AS ?max_v2)
        WHERE
          { { { GRAPH ?g2
                  { ?item  <http://www.wikidata.org/prop/direct/P625>  ?geoLocation }
                ?g2  <http://wikidata.org/deltas/direction>  "backwards"^^<http://www.w3.org/2001/XMLSchema#string> . 
                ?g2  <http://wikidata.org/deltas/version>  ?v2
              }
              BIND(str(?v2) AS ?v2_)
            }
            FILTER ( str(?v2) >= "20170830" )
          }
        GROUP BY ?item ?geoLocation
      }
      OPTIONAL
        { { SELECT  ?item ?geoLocation (strdt(MIN(?v3_), <http://www.w3.org/2001/XMLSchema#string>) AS ?max_v3)
            WHERE
              { { { GRAPH ?g3
                      { ?item  <http://www.wikidata.org/prop/direct/P625>  ?geoLocation }
                    ?g3  <http://wikidata.org/deltas/direction>  "forwards"^^<http://www.w3.org/2001/XMLSchema#string> . 
                    ?g3  <http://wikidata.org/deltas/version>  ?v3
                  }
                  BIND(str(?v3) AS ?v3_)
                }
                FILTER ( str(?v3) >= "20170830" )
              }
            GROUP BY ?item ?geoLocation
          }
          FILTER ( ( ! bound(?max_v3) ) || ( str(?max_v2) < str(?max_v3) ) )
        }
    }
    OPTIONAL
      { { SELECT  ?item ?image (strdt(MIN(?v4_), <http://www.w3.org/2001/XMLSchema#string>) AS ?max_v4)
          WHERE
            { { { GRAPH ?g4
                    { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }
                  ?g4  <http://wikidata.org/deltas/direction>  "backwards"^^<http://www.w3.org/2001/XMLSchema#string> . 
                  ?g4  <http://wikidata.org/deltas/version>  ?v4
                }
                BIND(str(?v4) AS ?v4_)
              }
              FILTER ( str(?v4) >= "20170830" )
            }
          GROUP BY ?item ?image
        }
        OPTIONAL
          { { SELECT  ?item ?image (strdt(MIN(?v5_), <http://www.w3.org/2001/XMLSchema#string>) AS ?max_v5)
              WHERE
                { { { GRAPH ?g5
                        { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }
                      ?g5  <http://wikidata.org/deltas/direction>  "forwards"^^<http://www.w3.org/2001/XMLSchema#string> . 
                      ?g5  <http://wikidata.org/deltas/version>  ?v5
                    }
                    BIND(str(?v5) AS ?v5_)
                  }
                  FILTER ( str(?v5) >= "20170830" )
                }
              GROUP BY ?item ?image
            }
            FILTER ( ( ! bound(?max_v5) ) || ( str(?max_v4) < str(?max_v5) ) )
          }
      }
    OPTIONAL
      { { { SELECT  ?item ?fr (strdt(MIN(?v6_), <http://www.w3.org/2001/XMLSchema#string>) AS ?max_v6)
            WHERE
              { { { GRAPH ?g6
                      { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr }
                    ?g6  <http://wikidata.org/deltas/direction>  "backwards"^^<http://www.w3.org/2001/XMLSchema#string> . 
                    ?g6  <http://wikidata.org/deltas/version>  ?v6
                  }
                  BIND(str(?v6) AS ?v6_)
                }
                FILTER ( str(?v6) >= "20170830" )
              }
            GROUP BY ?item ?fr
            HAVING ( lang(?fr) = "en" )
          }
          OPTIONAL
            { { SELECT  ?item ?fr (strdt(MIN(?v7_), <http://www.w3.org/2001/XMLSchema#string>) AS ?max_v7)
                WHERE
                  { { { GRAPH ?g7
                          { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr }
                        ?g7  <http://wikidata.org/deltas/direction>  "forwards"^^<http://www.w3.org/2001/XMLSchema#string> . 
                        ?g7  <http://wikidata.org/deltas/version>  ?v7
                      }
                      BIND(str(?v7) AS ?v7_)
                    }
                    FILTER ( str(?v7) >= "20170830" )
                  }
                GROUP BY ?item ?fr
                HAVING ( lang(?fr) = "en" )
              }
              FILTER ( ( ! bound(?max_v7) ) || ( str(?max_v6) < str(?max_v7) ) )
            }
        }
        FILTER ( lang(?fr) = "fr" )
      }
  }
;
```


## Version Delta conversions
### Independent Copies

```
SPARQL
SELECT  ?item ?fr ?geoLocation ?image
FROM <http://wikidata.org/20170830>
FROM NAMED <http://wikidata.org/20170823>
WHERE
  { { SELECT  ?item ?fr ?geoLocation ?image
      WHERE
        { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> ;
                 <http://www.wikidata.org/prop/direct/P625>  ?geoLocation
          OPTIONAL
            { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }
          OPTIONAL
            { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr
              FILTER ( lang(?fr) = "fr" )
            }
        }
    }
    MINUS
      { GRAPH <http://wikidata.org/20170823>
          { SELECT  ?item ?fr ?geoLocation ?image
            WHERE
              { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> ;
                       <http://www.wikidata.org/prop/direct/P625>  ?geoLocation
                OPTIONAL
                  { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }
                OPTIONAL
                  { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr
                    FILTER ( lang(?fr) = "fr" )
                  }
              }
          }}
  }
;
```

### Timestamp-based

```
SPARQL
SELECT  ?item ?fr ?geoLocation ?image
FROM <http://wikidata.org/intervals/20170809-20170830>
FROM <http://wikidata.org/intervals/20170809-20170907>
FROM <http://wikidata.org/intervals/20170809-20170913>
FROM <http://wikidata.org/intervals/20170809-20170920>
FROM <http://wikidata.org/intervals/20170809-20170927>
FROM <http://wikidata.org/intervals/20170816-20170830>
FROM <http://wikidata.org/intervals/20170816-20170907>
FROM <http://wikidata.org/intervals/20170816-20170913>
FROM <http://wikidata.org/intervals/20170816-20170920>
FROM <http://wikidata.org/intervals/20170816-20170927>
FROM <http://wikidata.org/intervals/20170823-20170830>
FROM <http://wikidata.org/intervals/20170823-20170907>
FROM <http://wikidata.org/intervals/20170823-20170913>
FROM <http://wikidata.org/intervals/20170823-20170920>
FROM <http://wikidata.org/intervals/20170823-20170927>
FROM <http://wikidata.org/intervals/20170830-20170830>
FROM <http://wikidata.org/intervals/20170830-20170907>
FROM <http://wikidata.org/intervals/20170830-20170913>
FROM <http://wikidata.org/intervals/20170830-20170920>
FROM <http://wikidata.org/intervals/20170830-20170927>
FROM NAMED <http://wikidata.org/intervals/20170809-20170823>
FROM NAMED <http://wikidata.org/intervals/20170809-20170830>
FROM NAMED <http://wikidata.org/intervals/20170809-20170907>
FROM NAMED <http://wikidata.org/intervals/20170809-20170913>
FROM NAMED <http://wikidata.org/intervals/20170809-20170920>
FROM NAMED <http://wikidata.org/intervals/20170809-20170927>
FROM NAMED <http://wikidata.org/intervals/20170816-20170823>
FROM NAMED <http://wikidata.org/intervals/20170816-20170830>
FROM NAMED <http://wikidata.org/intervals/20170816-20170907>
FROM NAMED <http://wikidata.org/intervals/20170816-20170913>
FROM NAMED <http://wikidata.org/intervals/20170816-20170920>
FROM NAMED <http://wikidata.org/intervals/20170816-20170927>
FROM NAMED <http://wikidata.org/intervals/20170823-20170823>
FROM NAMED <http://wikidata.org/intervals/20170823-20170830>
FROM NAMED <http://wikidata.org/intervals/20170823-20170907>
FROM NAMED <http://wikidata.org/intervals/20170823-20170913>
FROM NAMED <http://wikidata.org/intervals/20170823-20170920>
FROM NAMED <http://wikidata.org/intervals/20170823-20170927>
WHERE
  { { SELECT  ?item ?fr ?geoLocation ?image
      WHERE
        { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> ;
                 <http://www.wikidata.org/prop/direct/P625>  ?geoLocation
          OPTIONAL
            { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }
          OPTIONAL
            { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr
              FILTER ( lang(?fr) = "fr" )
            }
        }
    }
    MINUS
      { SELECT  ?item ?fr ?geoLocation ?image
        WHERE
          { GRAPH ?g0
              { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> }
            GRAPH ?g1
              { ?item  <http://www.wikidata.org/prop/direct/P625>  ?geoLocation }
            OPTIONAL
              { GRAPH ?g2
                  { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }
              }
            OPTIONAL
              { GRAPH ?g3
                  { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr }
                FILTER ( lang(?fr) = "fr" )
              }
          }
      }
  }
;
```

### Differential backups

```
SPARQL
SELECT  ?item ?fr ?geoLocation ?image
FROM <http://wikidata.org/20170809>
FROM <http://wikidata.org/deltas/20170830-20170809>
FROM NAMED <http://wikidata.org/deltas/20170809-20170830>
FROM NAMED <http://wikidata.org/20170809>
FROM NAMED <http://wikidata.org/deltas/20170823-20170809>
FROM NAMED <http://wikidata.org/deltas/20170809-20170823>
WHERE
  { { SELECT  ?item ?fr ?geoLocation ?image
      WHERE
        { { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> ;
                   <http://www.wikidata.org/prop/direct/P625>  ?geoLocation
            MINUS
              { GRAPH <http://wikidata.org/deltas/20170809-20170830>
                  { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> }}
          }
          MINUS
            { GRAPH <http://wikidata.org/deltas/20170809-20170830>
                { ?item  <http://www.wikidata.org/prop/direct/P625>  ?geoLocation }}
          OPTIONAL
            { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image
              MINUS
                { GRAPH <http://wikidata.org/deltas/20170809-20170830>
                    { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }}
            }
          OPTIONAL
            { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr
              MINUS
                { GRAPH <http://wikidata.org/deltas/20170809-20170830>
                    { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr }}
              FILTER ( lang(?fr) = "fr" )
            }
        }
    }
    MINUS
      { SELECT  ?item ?fr ?geoLocation ?image
        WHERE
          { {   { GRAPH <http://wikidata.org/20170809>
                    { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> }
                }
              UNION
                { GRAPH <http://wikidata.org/deltas/20170823-20170809>
                    { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> }
                }
              MINUS
                { GRAPH <http://wikidata.org/deltas/20170809-20170823>
                    { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> }}
            }
            {   { GRAPH <http://wikidata.org/20170809>
                    { ?item  <http://www.wikidata.org/prop/direct/P625>  ?geoLocation }
                }
              UNION
                { GRAPH <http://wikidata.org/deltas/20170823-20170809>
                    { ?item  <http://www.wikidata.org/prop/direct/P625>  ?geoLocation }
                }
              MINUS
                { GRAPH <http://wikidata.org/deltas/20170809-20170823>
                    { ?item  <http://www.wikidata.org/prop/direct/P625>  ?geoLocation }}
            }
            OPTIONAL
              {   { GRAPH <http://wikidata.org/20170809>
                      { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }
                  }
                UNION
                  { GRAPH <http://wikidata.org/deltas/20170823-20170809>
                      { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }
                  }
                MINUS
                  { GRAPH <http://wikidata.org/deltas/20170809-20170823>
                      { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }}
              }
            OPTIONAL
              { {   { GRAPH <http://wikidata.org/20170809>
                        { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr }
                    }
                  UNION
                    { GRAPH <http://wikidata.org/deltas/20170823-20170809>
                        { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr }
                    }
                  MINUS
                    { GRAPH <http://wikidata.org/deltas/20170809-20170823>
                        { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr }}
                }
                FILTER ( lang(?fr) = "fr" )
              }
          }
      }
  }
;
```

### Reverse-differential backups

```
SPARQL
SELECT  ?item ?fr ?geoLocation ?image
FROM <http://wikidata.org/20170927>
FROM <http://wikidata.org/deltas/20170830-20170927>
FROM NAMED <http://wikidata.org/deltas/20170927-20170830>
FROM NAMED <http://wikidata.org/20170927>
FROM NAMED <http://wikidata.org/deltas/20170823-20170927>
FROM NAMED <http://wikidata.org/deltas/20170927-20170823>
WHERE
  { { SELECT  ?item ?fr ?geoLocation ?image
      WHERE
        { { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> ;
                   <http://www.wikidata.org/prop/direct/P625>  ?geoLocation
            MINUS
              { GRAPH <http://wikidata.org/deltas/20170927-20170830>
                  { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> }}
          }
          MINUS
            { GRAPH <http://wikidata.org/deltas/20170927-20170830>
                { ?item  <http://www.wikidata.org/prop/direct/P625>  ?geoLocation }}
          OPTIONAL
            { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image
              MINUS
                { GRAPH <http://wikidata.org/deltas/20170927-20170830>
                    { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }}
            }
          OPTIONAL
            { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr
              MINUS
                { GRAPH <http://wikidata.org/deltas/20170927-20170830>
                    { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr }}
              FILTER ( lang(?fr) = "fr" )
            }
        }
    }
    MINUS
      { SELECT  ?item ?fr ?geoLocation ?image
        WHERE
          { {   { GRAPH <http://wikidata.org/20170927>
                    { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> }
                }
              UNION
                { GRAPH <http://wikidata.org/deltas/20170823-20170927>
                    { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> }
                }
              MINUS
                { GRAPH <http://wikidata.org/deltas/20170927-20170823>
                    { ?item  <http://www.wikidata.org/prop/direct/P463>  <http://www.wikidata.org/entity/Q1010307> }}
            }
            {   { GRAPH <http://wikidata.org/20170927>
                    { ?item  <http://www.wikidata.org/prop/direct/P625>  ?geoLocation }
                }
              UNION
                { GRAPH <http://wikidata.org/deltas/20170823-20170927>
                    { ?item  <http://www.wikidata.org/prop/direct/P625>  ?geoLocation }
                }
              MINUS
                { GRAPH <http://wikidata.org/deltas/20170927-20170823>
                    { ?item  <http://www.wikidata.org/prop/direct/P625>  ?geoLocation }}
            }
            OPTIONAL
              {   { GRAPH <http://wikidata.org/20170927>
                      { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }
                  }
                UNION
                  { GRAPH <http://wikidata.org/deltas/20170823-20170927>
                      { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }
                  }
                MINUS
                  { GRAPH <http://wikidata.org/deltas/20170927-20170823>
                      { ?item  <http://www.wikidata.org/prop/direct/P18>  ?image }}
              }
            OPTIONAL
              { {   { GRAPH <http://wikidata.org/20170927>
                        { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr }
                    }
                  UNION
                    { GRAPH <http://wikidata.org/deltas/20170823-20170927>
                        { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr }
                    }
                  MINUS
                    { GRAPH <http://wikidata.org/deltas/20170927-20170823>
                        { ?item  <http://www.w3.org/2000/01/rdf-schema#label>  ?fr }}
                }
                FILTER ( lang(?fr) = "fr" )
              }
          }
      }
  }
;
```
