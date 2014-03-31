package test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.Document;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;


public class BookMain {

  private static final String BOOKSTORE_XML = "./bookstore-jaxb.xml";

  public static void main(String[] args) throws JAXBException, IOException {

    ArrayList<Book> bookList = new ArrayList<Book>();

    // create books
    Book book1 = new Book();
    book1.setIsbn("978-0060554736");
    book1.setName("The Game");
    book1.setAuthor("Neil Strauss");
    book1.setPublisher("Harpercollins");
    book1.setThingy("thingy1");
    bookList.add(book1);

    Book book2 = new Book();
    book2.setIsbn("978-3832180577");
    book2.setName("Feuchtgebiete");
    book2.setAuthor("Charlotte Roche");
    book2.setPublisher("Dumont Buchverlag");
    book2.setThingy("thingy2");
    bookList.add(book2);

    // create bookstore, assigning book
    Bookstore bookstore = new Bookstore();
    bookstore.setName("Fraport Bookstore");
    bookstore.setLocation("Frankfurt Airport");
    bookstore.setZorks(bookList);

    // create JAXB context and instantiate marshaller
    JAXBContext context = JAXBContext.newInstance(Bookstore.class);
    Marshaller m = context.createMarshaller();
    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
    
    

    // Write to System.out
    JAXBElement<Bookstore> je2 = new JAXBElement<Bookstore>(new QName("bookstore"), Bookstore.class, bookstore);
    m.marshal(je2, System.out);
    
    final List results = new ArrayList();
    
    context.generateSchema(
            // need to define a SchemaOutputResolver to store to
            new SchemaOutputResolver()
            {
                @Override
                public Result createOutput( String ns, String file )
                        throws IOException
                {
                    // save the schema to the list
                    DOMResult result = new DOMResult();
                    result.setSystemId( file );
                    results.add( result );
                    return result;
                }
            } );

 // output schema via System.out
    DOMResult domResult = (DOMResult)results.get( 0 );
    Document doc = (Document) domResult.getNode();
    OutputFormat format = new OutputFormat( doc );
    format.setIndenting( true );
    XMLSerializer serializer = new XMLSerializer( System.out, format );
    serializer.serialize( doc );
    
    // Write to File
    m.marshal(bookstore, new File(BOOKSTORE_XML));

    // get variables from our xml file, created before
    System.out.println();
    System.out.println("Output from our XML File: ");
    Unmarshaller um = context.createUnmarshaller();
    Bookstore bookstore2 = (Bookstore) um.unmarshal(new FileReader(BOOKSTORE_XML));
    List<Book> list = bookstore2.getZorks();
    for (Book book : list) {
      System.out.println("Book: " + book.getName() + " from "
          + book.getAuthor());
    }
  }
} 