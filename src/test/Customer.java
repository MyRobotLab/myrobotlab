package test;

import java.util.List;

public class Customer {
 
    private String firstName;
    private String lastName;
    private List<PhoneNumber> phoneNumbers;
 
    public String getFirstName() {
        return firstName;
    }
 
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
 
    public String getLastName() {
        return lastName;
    }
 
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
 
    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }
 
    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }
 
}