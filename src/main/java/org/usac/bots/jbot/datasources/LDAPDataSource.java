package org.usac.bots.jbot.datasources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.stereotype.Repository;
import org.usac.bots.jbot.entities.LDAPPerson;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
@ConfigurationProperties(prefix = "ldap")
public class LDAPDataSource extends LogGrootDataSource {

    private int maxSearchResults;

    @Autowired
    public LDAPDataSource(LdapTemplate ldapTemplate) {

        this.ldapTemplate = ldapTemplate;

        try {
            getPersonByName("abcdefghijklmnopqrstuvwxyz");
        }
        catch (Exception e) {
            setEnabled(false);
            log.error(e.getMessage());
        }
    }

    public int getMaxSearchResults() {
        return maxSearchResults;
    }

    public void setMaxSearchResults(int maxSearchResults) {
        this.maxSearchResults = maxSearchResults;
    }



    private class PersonAttributesMapper implements AttributesMapper<LDAPPerson> {
        public LDAPPerson mapFromAttributes(Attributes attrs) throws NamingException {
            LDAPPerson person = new LDAPPerson();
            if (attrs.get("distinguishedName") != null) person.setDistinguishedName((String) attrs.get("distinguishedName").get());
            if (attrs.get("givenName") != null) person.setFirstName((String) attrs.get("givenName").get());
            if (attrs.get("sn") != null) person.setLastName((String) attrs.get("sn").get());
            if (attrs.get("title") != null) person.setTitle((String) attrs.get("title").get());
            if (attrs.get("mail") != null) person.setEmail((String) attrs.get("mail").get());
            if (attrs.get("mobile") != null) person.setMobile((String) attrs.get("mobile").get());
            if (attrs.get("telephoneNumber") != null) person.setOffice((String) attrs.get("telephoneNumber").get());

            if (attrs.get("manager") != null) {
                LdapName managerDn = new LdapName((String) attrs.get("manager").get());
                person.setManager(managerDn.getRdn(managerDn.size() - 1).getValue().toString());
            }
            else if (attrs.get("managedBy") != null) {
                LdapName managerDn = new LdapName((String) attrs.get("managedBy").get());
                person.setManager(managerDn.getRdn(managerDn.size() - 1).getValue().toString());
            }
            else {
                person.setManager("no one");
            }

            return person;
        }
    }


    private final LdapTemplate ldapTemplate;

    @PostConstruct
    public void init() {
        log.info("Initializing LDAP");
    }


    public List<LDAPPerson> getPersonByName(String name) {

        // TODO: Make bots have their service accounts
        if (name.equalsIgnoreCase("J.A.R.V.I.S.")) {
            LDAPPerson bot = new LDAPPerson();
            bot.setDistinguishedName("J.A.R.V.I.S.");
            bot.setEmail("JarvisHC@webex.bot");
            bot.setFirstName("J.A.R.V.I.S.");
            bot.setLastName("");
            bot.setTitle("Deployment Management Bot");
            return Collections.singletonList(bot);
        }

        if (name.equalsIgnoreCase("LogGroot")) {
            LDAPPerson bot = new LDAPPerson();
            bot.setDistinguishedName("LogGroot");
            bot.setEmail("loggroot@webex.bot");
            bot.setFirstName("LogGroot");
            bot.setLastName("");
            bot.setTitle("Business Automation Bot");
            return Collections.singletonList(bot);
        }


        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setTimeLimit(3000);
        sc.setCountLimit(maxSearchResults);
        sc.setReturningAttributes(new String[]{"distinguishedName", "givenName", "cn", "sn", "mail", "title", "manager", "mobile", "telephoneNumber"});


        String filter = "(&(objectClass=person)(|(sn=*" + name + "*)(cn=*" + name + "*)(givenName=*" + name + "*)))";
        return ldapTemplate.search(LdapUtils.emptyLdapName(), filter, sc, new PersonAttributesMapper());

    }



    public List<LDAPPerson> getPersonByEmail(String email) {

        if (email == null) return new ArrayList<>();

        // TODO: Make bots have their service accounts
        if (email.equalsIgnoreCase("JarvisHC@webex.bot")) {
            LDAPPerson bot = new LDAPPerson();
            bot.setDistinguishedName("J.A.R.V.I.S.");
            bot.setEmail("JarvisHC@webex.bot");
            bot.setFirstName("J.A.R.V.I.S.");
            bot.setLastName("");
            bot.setTitle("Deployment Management Bot");
            return Collections.singletonList(bot);
        }

        if (email.equalsIgnoreCase("loggroot@webex.bot")) {
            LDAPPerson bot = new LDAPPerson();
            bot.setDistinguishedName("LogGroot");
            bot.setEmail("loggroot@webex.bot");
            bot.setFirstName("LogGroot");
            bot.setLastName("");
            bot.setTitle("Business Automation Bot");
            return Collections.singletonList(bot);
        }


        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setTimeLimit(3000);
        sc.setCountLimit(maxSearchResults);
        sc.setReturningAttributes(new String[]{"distinguishedName", "givenName", "cn", "sn", "mail", "title", "manager", "mobile", "telephoneNumber"});


        String filter = "(&(objectClass=person)(mail=*" + email + "*))";
        return ldapTemplate.search(LdapUtils.emptyLdapName(), filter, sc, new PersonAttributesMapper());

    }

    public List<LDAPPerson> getGroupByName(String name) {
        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setTimeLimit(30000);
        sc.setCountLimit(maxSearchResults);
        sc.setReturningAttributes(new String[]{"distinguishedName", "givenName", "cn", "sn", "mail", "title", "manager", "managedBy", "mobile", "telephoneNumber"});


        String filter = "(&(objectClass=group)(name=*" + name + "*))";
        return ldapTemplate.search(LdapUtils.emptyLdapName(), filter, sc, new PersonAttributesMapper());

    }



    public List<LDAPPerson> getPersonsByTitle(String name) {

        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setTimeLimit(3000);
        sc.setCountLimit(maxSearchResults);
        sc.setReturningAttributes(new String[]{"distinguishedName", "givenName", "cn", "sn", "mail", "title", "manager", "mobile", "telephoneNumber"});


        String filter = "(&(objectClass=person)(|(sn=*" + name + "*)(cn=*" + name + "*)(title=*" + name + "*)))";
        return ldapTemplate.search(LdapUtils.emptyLdapName(), filter, sc, new PersonAttributesMapper());

    }

    public List<LDAPPerson> getDirectReportsOfManager(LDAPPerson manager) {


        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setTimeLimit(3000);
        sc.setCountLimit(maxSearchResults);
        sc.setReturningAttributes(new String[]{"distinguishedName", "givenName", "cn", "sn", "mail", "title", "manager", "mobile", "telephoneNumber"});


        String filter = "(&(objectClass=person)(manager=" + manager.getDistinguishedName() + "))";
        return ldapTemplate.search(LdapUtils.emptyLdapName(), filter, sc, new PersonAttributesMapper());

    }


}
