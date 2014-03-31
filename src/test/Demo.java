package test;

import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

public class Demo {

   public static void main(String[] args) throws Exception {
       JAXBContext jc = JAXBContext.newInstance(Customer.class);

       /*
       StreamSource xml = new StreamSource("input.xml");
       Unmarshaller unmarshaller = jc.createUnmarshaller();
       JAXBElement<Customer> je1 = unmarshaller.unmarshal(xml, Customer.class);
       Customer customer = je1.getValue();
       */
       
       
       Customer c = new Customer();
       c.setFirstName("greg");
       c.setLastName("perry");
       ArrayList<PhoneNumber> al = new ArrayList<PhoneNumber>();
       PhoneNumber phone = new PhoneNumber();
       phone.setNumber("3444959");
       phone.setType("mobile");
       al.add(phone);
       c.setPhoneNumbers(al);

       JAXBElement<Customer> je2 = new JAXBElement<Customer>(new QName("customer"), Customer.class, c);
       Marshaller marshaller = jc.createMarshaller();
       marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
       marshaller.marshal(je2, System.out);
   }

}