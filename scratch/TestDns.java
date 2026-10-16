import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

public class TestDns {
    public static void main(String[] args) {
        String[] testDomains = {"gmail.com", "yahoo.com", "luly.colm", "fake123nonexistentdomain.com", "gmail.con"};
        for (String domain : testDomains) {
            boolean valid = checkDomain(domain);
            System.out.println("Domain: " + domain + " -> Valid MX: " + valid);
        }
    }

    public static boolean checkDomain(String domain) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "2000");
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});
            if (attrs != null && attrs.get("MX") != null && attrs.get("MX").size() > 0) {
                return true;
            }
            // fallback check A record
            attrs = ctx.getAttributes(domain, new String[]{"A"});
            return attrs != null && attrs.get("A") != null && attrs.get("A").size() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
