package wiki.uqracing.macro;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.*;
import org.jsoup.helper.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class TableFunctions implements Macro {

    public String execute(Map<String, String> params, String body, ConversionContext conversionContext) throws MacroExecutionException {
        Document doc = Jsoup.parse(body);

        Elements tableBodies = doc.select("body > div > table > tbody");

        for (Element tableBody : tableBodies) {
            Table table = new Table(tableBody);
            table.replaceCells();
            tableBody.html(table.body.html());
        }

        return doc.select("html > body").html();
    }

    public BodyType getBodyType() { return BodyType.RICH_TEXT; }

    public OutputType getOutputType() { return OutputType.BLOCK; }
}
