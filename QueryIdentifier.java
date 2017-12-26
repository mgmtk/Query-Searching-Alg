package utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QueryIdentifier
{
  String query;
  ArrayList<String> stringSearchTerms;
  ArrayList<SearchTerm> searchTerms;
  Boolean advanced;
  String[] stopList = new String[] {"a", "an", "and", "are", "but", "did", "do", "does", "for",
      "had", "has", "is", "it", "its", "of", "or", "that", "the", "this", "to", "were", "which",
      "with"};

  public QueryIdentifier(String query)
  {

    advanced = false;
    this.query = query;
    indexQuery();

  }

  private void indexQuery()
  {

    stringSearchTerms = new ArrayList<String>(Arrays.asList(query.split(" ")));
    ArrayList<String> temp = new ArrayList<String>();
    for(String term: stringSearchTerms){
      temp.add(term.toLowerCase());
     
    }
    stringSearchTerms = temp;
    if (!query.contains("^"))
    {

      removeStopTerms();
    }
    else
    {
      advanced = true;
    }
    reduce();
    searchTerms = new ArrayList<SearchTerm>();
    for (String term : stringSearchTerms)
    {
      SearchTerm searchTerm = new SearchTerm(term);
      searchTerms.add(searchTerm);

    }
  }

  private void reduce()
  {

    for (int i = stringSearchTerms.size() -1; i >= 100; i--)
    {
      stringSearchTerms.remove(i);

    }

  }

  private void removeStopTerms()
  {

    ArrayList<String> temp = new ArrayList<String>();
    for (int i = 0; i < stringSearchTerms.size(); i++)
    {

      if (!termCheck(stringSearchTerms.get(i))) 
      {
        temp.add(stringSearchTerms.get(i));
       
      }

    }
    stringSearchTerms = temp;
    temp = null;

   

  }

  private boolean termCheck(String term)
  {
    for (int i = 0; i < stopList.length; i++)
    {
      if (term.equals(stopList[i]))
      {
        return true;

      }

    }
    return false;
  }

  public Boolean isAdvanced()
  {

    return advanced;
  }

  public ArrayList<SearchTerm> getIndexedQuery()
  {

    return searchTerms;
  }

}

