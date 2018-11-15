package org.usac.bots.jbot.entities;

public class LDAPPerson {

    private String distinguishedName;
    private String firstName;
    private String lastName;
    private String email;
    private String mobile;
    private String office;
    private String title;
    private String manager;

    public LDAPPerson() {
    }

    public String getDistinguishedName() {
        return distinguishedName;
    }

    public void setDistinguishedName(String distinguishedName) {
        this.distinguishedName = distinguishedName;
    }

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getOffice() {
        return office;
    }

    public void setOffice(String office) {
        this.office = office;
    }

    public static String formatPhoneNumber(String phone) {
        if (phone != null) {
            phone = phone.replaceAll("[^0-9]", "");
            if (phone.length() == 10) {
                phone = "+1 (" + phone.substring(0, 3) + ") " + phone.substring(3, 6) + "-" + phone.substring(6);
            }
        }
        return phone;
    }

    @Override
    public String toString() {
        return "LDAPPerson{" +
                "distinguishedName='" + distinguishedName + '\'' +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", title='" + title + '\'' +
                ", email='" + email + '\'' +
                ", manager='" + manager + '\'' +
                ", mobile='" + mobile + '\'' +
                ", office='" + office + '\'' +

                '}';
    }
}
