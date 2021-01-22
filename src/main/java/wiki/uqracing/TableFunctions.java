package wiki.uqracing.macro;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;

import java.util.Map;

public class TableFunctions implements Macro {

    public String execute(Map<String, String> params, String body, ConversionContext conversionContext) throws MacroExecutionException {
        return body;
    }

    public BodyType getBodyType() { return BodyType.RICH_TEXT; }

    public OutputType getOutputType() { return OutputType.BLOCK; }
}
