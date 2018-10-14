package tools;
import com.sendgrid.*;
import java.io.IOException;

/**
 * Created by SpereShelde on 2018/6/23.
 */
public class MailBySendgrid {

    private String from;
    private String to;
    private String subject;
    private String content;
    private String key;

    public MailBySendgrid(String from, String to, String subject, String content, String key) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.content = content;
        this.key = key;
    }

    public boolean send() throws IOException {
        Email from = new Email(this.from);
        String subject = this.subject;
        Email to = new Email(this.to);
        Content content = new Content("text/plain", this.content);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(this.key);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            if (response.getStatusCode() == 202){
                return true;
            }else{
                return false;
            }
        } catch (IOException ex) {
            throw ex;
        }
    }
}
