package ethanjones.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DataFormatter {

    /*
        [ - Array
        ( - ArrayList
        { - HashMap
        < - DataGroup
     */

    private static final String indentStr = "  ";

    public static String str(Object obj) {
        return str(obj, "", new StringBuilder()).toString();
    }

    private static StringBuilder str(Object obj, String indent, StringBuilder stringBuilder) {
        if (obj.getClass().isArray()) {
            stringBuilder.append("[");
            String newIndent = indent + indentStr;
            for (Object object : ((Object[]) obj)) {
                stringBuilder.append("\n").append(newIndent);
                str(object, newIndent, stringBuilder);
            }
            stringBuilder.append("\n").append(indent).append("]");
        } else if (obj instanceof ArrayList) {
            stringBuilder.append("(");
            String newIndent = indent + indentStr;
            for (Object object : ((ArrayList) obj)) {
                stringBuilder.append("\n").append(newIndent);
                str(object, newIndent, stringBuilder);
            }
            stringBuilder.append("\n").append(indent).append(")");
        } else if (obj instanceof HashMap) {
            stringBuilder.append("{");
            String newIndent = indent + indentStr;
            for (Object o : ((HashMap) obj).entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                stringBuilder.append("\n").append(newIndent);
                str(entry.getKey(), newIndent, stringBuilder);
                stringBuilder.append(":");
                str(entry.getValue(), newIndent, stringBuilder);
            }
            stringBuilder.append("\n").append(indent).append("}");
        } else if (obj instanceof DataGroup) {
            stringBuilder.append("<");
            String newIndent = indent + indentStr;
                for (Map.Entry<String, Object> entry : ((DataGroup) obj).entrySet()) {
                    stringBuilder.append("\n").append(newIndent).append(entry.getKey()).append(":");
                    str(entry.getValue(), newIndent, stringBuilder);
                }
            stringBuilder.append("\n").append(indent).append(">");
        } else {
            stringBuilder.append(obj.toString());
        }
        return stringBuilder;
    }
}
