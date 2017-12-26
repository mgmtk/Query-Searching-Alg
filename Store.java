package store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import store.collection.Collection;
import store.collection.Document;
import store.collection.LoadSummary;
import store.collection.OPUS;
import store.index.IndexTerm;
import store.index.Posting;
import utilities.QueryIdentifier;
import utilities.SearchTerm;

/**
 * The ultimate handler of all data related processes. Represents the entire model of the Pirex
 * application and contains a reference to a PirexCollection of Pirex OPUS objects and their
 * PirexDocuments loaded in by the user. The PirexModel has the ability to add new PirexOPUS objects
 * (so long as they are not duplicates) and remove PirexOPUS objects if they are present in the
 * PirexCollection. Also is able to search for all the PirexDocuments within the PirexCollection
 * that satisfy a given string query.
 * 
 * Since this is the root class of the entire PirexModel system, it is able to be directly
 * serialized and deserialized to the file system.
 * 
 * @author Mark Conigliaro
 * @version 1.0
 *
 */
public class Store implements Serializable
{
  public static final String FILE_NAME = "pirex_library";
  public static final String PLF = ".plf";

  private static final String SERIALIZATION_ERROR = "Error serializing the model.";
  private static final long serialVersionUID = 1L;

  private String filePath;
  private Collection collection;

  private static final String[] STOP_LIST = {"a", "an", "and", "are", "but", "did", "do", "does",
      "for", "had", "has", "is", "it", "its", "of", "or", "that", "the", "this", "to", "were",
      "which", "with"};

  /**
   * Creates a new empty PirexModel. Initializes the collection and index and associates them.
   * 
   * @param filePath
   *          File path to serialize to.
   */
  public Store(String filePath)
  {
    this.filePath = filePath;
    this.collection = new Collection();
  }

  /**
   * Getter method for the collection.
   * 
   * @return the collection
   */
  public Collection getCollection()
  {
    return this.collection;
  }

  /**
   * Getter method for the file path.
   * 
   * @return the file path.
   */
  public String getFilePath()
  {
    return this.filePath;
  }

  /**
   * Attempts to add a new OPUS into the model. Returns a load summary on success.
   * 
   * @param opus
   *          The OPUS to be added to the model.
   * @return A PirexLoadSummary if successful, null if otherwise.
   */
  public LoadSummary addOPUS(OPUS opus)
  {
    LoadSummary summary = this.getCollection().addOPUS(opus);

    try
    {
      serializePirexModel(this.getFilePath());
    }
    catch (IOException e)
    {
      System.err.println(SERIALIZATION_ERROR);
    }

    return summary;
  }

  /**
   * Attempts to remove the OPUS from the collection and all of it's associated postings from the
   * index. Removes an OPUS by it's ordinal number.
   * 
   * @param opusOrdinalNumber
   *          The ordinal number of the OPUS to be removed.
   * @return True if successfully removed. False if otherwise.
   */
  public boolean removeOPUS(int opusOrdinalNumber)
  {
    boolean status = this.getCollection().removeOPUS(opusOrdinalNumber);

    try
    {
      serializePirexModel(this.getFilePath());
    }
    catch (IOException e)
    {
      System.err.println(SERIALIZATION_ERROR);
    }

    return status;
  }

  /**
   * Removes all the OPI from the collection and entries from the index.
   */
  public void purgeModel()
  {
    this.getCollection().removeAllOPI();

    try
    {
      serializePirexModel(this.getFilePath());
    }
    catch (IOException e)
    {
      System.err.println(SERIALIZATION_ERROR);
    }
  }

  /**
   * Searches for all documents that contain the search terms within a string query. Returns an
   * ArrayList of all documents found within the collection.
   * 
   * @param query
   *          The string query to search for document postings.
   * @return ArrayList of all documents that match the query search terms.
   */
  public ArrayList<Document> searchModel(QueryIdentifier Query)
  {
    ArrayList<Document> documents = new ArrayList<Document>();
    ArrayList<SearchTerm> searchTerms = Query.getIndexedQuery();
    ArrayList<Document> docsToSearch = getAllDocs(); // gets all documents contained in every
                                                     // opus

    for (int i = 0; i < searchTerms.size(); i++)
    {
      if (searchTerms.get(i).isWord())
      {
        if (documents.isEmpty())
        {
          for (Document doc : docsToSearch)
          {
            if (doc.getText().toLowerCase().contains(searchTerms.get(i).getTerm()))
            {

              documents.add(doc);
            }

          }

        }
        else
        { // if document array is not empty it will check the current documents against the new
          // search term
          ArrayList<Document> temp = new ArrayList<Document>();
          for (int j = 0; j < documents.size(); j++)
          {
            if ((documents.get(j).getText().toLowerCase().contains(searchTerms.get(i).getTerm())))
            {

              temp.add(documents.get(j));
            }
          }
          documents = temp;
        }

      }
      else if (searchTerms.get(i).notTerm()) // if not term removes all documents that contain the
                                             // next term.
      {
        if (!documents.isEmpty())
        {
          ArrayList<Document> temp = new ArrayList<Document>();
          for (int j = 0; j < documents.size(); j++)
          {
            if (!(documents.get(j).getText().toLowerCase().contains(searchTerms.get(i + 1).getTerm())))
            {

              temp.add(documents.get(j));
            }
          }
          documents = temp;
        }
        else
        {
          for (Document doc : docsToSearch)
          {
            if (!doc.getText().toLowerCase().contains(searchTerms.get(i + 1).getTerm()))
            {

              documents.add(doc);
            }

          }

        }
        searchTerms.remove(i + 1);
      }
      else if (searchTerms.get(i).adjacencyTerm())
      {
        String adjacency = searchTerms.get(i - 1).getTerm() + " " + searchTerms.get(i + 1).getTerm();
        ArrayList<Document> temp = new ArrayList<Document>();
        for (int j = 0; j < documents.size(); j++)
        {
          if ((documents.get(j)).getText().toLowerCase().contains(adjacency))
          {
            temp.add(documents.get(j));

          }

        }
        documents = temp;
        searchTerms.remove(i + 1);
      }

    }
    return documents;
  }

  private ArrayList<Document> getAllDocs()
  {
    ArrayList<Document> docsToSearch = new ArrayList<Document>();
    for (OPUS opus : this.collection.getOPI())
    {
      for (Document doc : opus.getDocuments())
      {
        docsToSearch.add(doc);

      }
    }
    return docsToSearch;
  }

  /**
   * Summarizes the model and returns it as a formatted string containing information on the current
   * state of the model.
   * 
   * @return a String summary of the model.
   */
  public String summarizeModel()
  {
    String summary = "";

    // Create a summary for each OPUS in the collection.
    String opusSummary;
    for (OPUS opus : this.getCollection().getOPI())
    {
      opusSummary = String.format("OPUS %d: %s\t%s\t%d documents\n\t\t%s\n",
          opus.getOrdinalNumber(), opus.getAuthor(), opus.getTitle(), opus.getNumberOfDocuments(),
          opus.getFilePath());
      summary += opusSummary;
    }

    // Append index summary.
    summary += String.format("\nIndex Terms: %d\nPostings: %d", this.getNumberOfEntries(),
        this.getTotalNumberOfPostings());

    return summary;
  }

  /**
   * Returns the number of OPI in the model's collection.
   * 
   * @return the number of OPI in the model's collection.
   */
  public int getNumberOfOPI()
  {
    return this.getCollection().getNumberOfOPI();
  }

  /**
   * Returns the total number of documents in the model's collection.
   * 
   * @return the total number of documents in the model's collection.
   */
  public int getTotalNumberOfDocuments()
  {
    return this.getCollection().getTotalNumberOfDocuments();
  }

  /**
   * Returns the number of entries in the model's collection index.
   * 
   * @return the number of entries in the model's collection index.
   */
  public int getNumberOfEntries()
  {
    return this.getCollection().getIndex().getNumberOfTerms();
  }

  /**
   * Returns the total number of postings in the model's collection index.
   * 
   * @return the total number of postings in the model's collection index.
   */
  public int getTotalNumberOfPostings()
  {
    return this.getCollection().getIndex().getTotalNumberOfPostings();
  }

  /**
   * Serializes the PirexModel to a Pirex Library File (.plf) on the file system at a given path.
   * 
   * @param path
   *          The directory where the PLF will be saved.
   * @throws IOException
   *           On file output error.
   */
  public void serializePirexModel(String path) throws IOException
  {
    File outputFile;

    if (path.length() > 0)
    {
      File saveDirectory = new File(path);
      saveDirectory.mkdir();
      outputFile = new File(saveDirectory + FILE_NAME + PLF);
    }
    else
    {
      outputFile = new File(FILE_NAME + PLF);
    }

    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFile));
    oos.writeObject(this);
    oos.close();
  }

  /**
   * Deserializes a Pirex Library File (.plf) into a PirexModel object.
   * 
   * @param file
   *          The PLF file to be deserialized.
   * @return The deserialized PirexModel object.
   * @throws IOException
   *           On file input error.
   * @throws FileNotFoundException
   *           On file search failure.
   * @throws ClassNotFoundException
   *           On object casting error.
   */
  public static Store deserializePirexModel(File file)
      throws FileNotFoundException, IOException, ClassNotFoundException
  {
    Store returnPirexModel = null;

    String filename = file.getName();
    if (filename.substring(filename.lastIndexOf('.'), filename.length()).equals(PLF))
    {
      ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
      returnPirexModel = (Store) ois.readObject();
      ois.close();
    }
    else
      throw new IOException("File type not supported.");

    return returnPirexModel;
  }
}
