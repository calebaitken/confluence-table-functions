package wiki.uqracing.macro;

import java.util.ArrayList;
import java.util.List;

import java.lang.Character;
import java.util.stream.*;
import java.util.Arrays;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.Integer;
import java.lang.StringBuffer;

import org.jsoup.*;
import org.jsoup.helper.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class Table {
    // solo cellRef regex
    Pattern cellRef = Pattern.compile("(?<![A-Z0-9:])([A-Z])([0-9]+)(?![A-Z0-9:])", Pattern.CASE_INSENSITIVE);

    // sum function regex
    Pattern sumFunct = Pattern.compile("SUM\\(((([A-Z])([0-9]+)):(([A-Z])([0-9]+)))\\)", Pattern.CASE_INSENSITIVE);

    // tbody element
    public Element body;

    public Table() { }

    public Table(Element tableBody) {
        this.body = tableBody;
    }

    public void replaceCells() {
        // for each row in table
        Elements tableRows = this.body.select(">tr");
        for (Element tableRow : tableRows) {
            // for each cell in row
            Elements tableCells = tableRow.select(">*");
            for (Element tableCell : tableCells) {
                // process
                this.replaceCell(tableCell);
            }
        }
    }

    private Element replaceCell(Element cell) {
        // only replace content if it starts with an equal sign
        if (cell.html().charAt(0) == '=') {
            StringBuffer sb = new StringBuffer();
            Matcher m;

            // remove leading equal sign
            cell.html(cell.html().replaceAll("^=", ""));

            // process cell references
            m = cellRef.matcher(cell.html());
            while(m.find()) {
                String value = this.replaceCell(getCell(m.group(1).charAt(0), Integer.parseInt(m.group(2)))).html();
                m.appendReplacement(sb, value);
            }
            m.appendTail(sb);
            cell.html(sb.toString());
            sb.setLength(0); // garbage collector tyvm

            // process sum function
            m = sumFunct.matcher(cell.html());
            while(m.find()) {
                // get list of cells in array
                List<Element> array = getCellArray(getCell(m.group(3).charAt(0), Integer.parseInt(m.group(4))),
                                                   getCell(m.group(6).charAt(0), Integer.parseInt(m.group(7))));

                // convert elements to string
                List<String> sArr = new ArrayList<>();
                for (Element e : array) {
                    sArr.add(replaceCell(getCell(cellPos(e).charAt(0), Character.getNumericValue(cellPos(e).charAt(1)))).html());
                }

                // join with plus operator
                String value = String.join("+", sArr);
                m.appendReplacement(sb, "(" + value + ")");
            }
            m.appendTail(sb);
            cell.html(sb.toString());
            sb.setLength(0); // garbage collector tyvm

            // eval math
            try {
                cell.html(eval(cell.html()) + "");
            } catch (RuntimeException e) {
                throw new RuntimeException("Cannot parse cell " + cellPos(cell), e);
            }
        }

        return cell;
    }

    private String cellPos(Element cell) {
        int coln = cell.elementSiblingIndex();
        int rown = cell.parent().elementSiblingIndex() + 1;
        String result = "" + Character.toUpperCase((char)(coln + 'a')) + (char)(rown + '0'); // convert int to alpha value and int to char value
        return result;
    }

    private Element getCell(char col, int row) {
        int coln = Character.toLowerCase(col) - 'a' + 1; // convert alpha to int value
        return this.body.select(">tr:eq(" + (row - 1) + ")").select(">*:eq(" + (coln - 1) + ")").first();
    }

    private List<Element> getCellArray(Element first, Element last) {
        List<Element> result = new ArrayList<>();
        String startPos = cellPos(first);
        String endPos = cellPos(last);
        
        int startCol = Character.toLowerCase(startPos.charAt(0)) - 'a' + 1; // convert alpha to int value
        int startRow = Character.getNumericValue(startPos.charAt(1)); // char to int value
        int endCol = Character.toLowerCase(endPos.charAt(0)) - 'a' + 1; // convert alpha to int value
        int endRow = Character.getNumericValue(endPos.charAt(1)); // char to int value

        // swap the start and end if needed
        if (startRow > endRow) {
            swap(startRow, endRow);
        }
        if (startCol > endCol) {
            swap(startCol, endCol);
        }

        // for each col
        for (int col = startCol; col <= endCol; ++col) {
            // for each row
            for (int row = startRow; row <= endRow; ++row) {
                // get cell
                result.add(getCell((char)(col + 'a' - 1), row));
            }
        }

        return result;
    }

    void swap(int a, int b) {
        int temp = a;
        a = b;
        b = temp;
    }

    @Override
    public String toString() {
        return this.body.toString();
    }


    // https://stackoverflow.com/a/26227947
    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;
    
            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }
    
            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }
    
            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }
    
            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            //        | number | functionName factor | factor `^` factor
    
            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }
    
            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }
    
            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus
    
                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    // functions with numbers/equations
                    x = parseFactor();
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else throw new RuntimeException("Unknown function: " + func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }
    
                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation
    
                return x;
            }
        }.parse();
    }
}