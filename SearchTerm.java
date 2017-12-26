package utilities;

public class SearchTerm
{
  String term;
  Boolean adjacencyTerm;
  Boolean notTerm;
  Boolean word;
  public SearchTerm(String term){
    
    this.term = term;
    adjacencyTerm = false;
    notTerm = false;
    word = false;
    checkTerm();
   
    
  }
  private void checkTerm()
  {
    if(term.equals("^")){
      adjacencyTerm = true;
      
    }
    else if(term.equals("~")){
      notTerm = true;
      
    }
    else{
      
      word = true;
    }
   
    
  }
  public boolean isWord(){
     
    return word;
    
  }
  public boolean adjacencyTerm(){
    
    return adjacencyTerm;
  }
  public boolean notTerm(){
    
    return notTerm;
  }
  public String getTerm(){
    
    return term;
  }

}
