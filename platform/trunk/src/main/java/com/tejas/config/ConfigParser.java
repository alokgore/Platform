package com.tejas.config;

import static com.tejas.config.ConfigParser.ParseState.EXPECTING_COMMA;
import static com.tejas.config.ConfigParser.ParseState.EXPECTING_EQUALS;
import static com.tejas.config.ConfigParser.ParseState.EXPECTING_ITEM;
import static com.tejas.config.ConfigParser.ParseState.EXPECTING_KEY;
import static com.tejas.config.ConfigParser.ParseState.EXPECTING_SEMICOLON;
import static com.tejas.config.ConfigParser.ParseState.EXPECTING_VALUE;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

public class ConfigParser
{
	enum ParseState
	{
		UNDEFINED,
		EXPECTING_KEY,
		EXPECTING_COMMA,
		EXPECTING_EQUALS,
		EXPECTING_ITEM,
		EXPECTING_SEMICOLON,
		EXPECTING_VALUE,
		;
	}
	
    public static List<ConfigEntry> parseConfigFile(String configFilePath) throws java.io.FileNotFoundException, java.io.IOException
    {
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(configFilePath)));
        return (new ConfigParser(r, configFilePath)).getConfigEntries();
    }

    private String _file = null;
    private BufferedReader _reader = null;
    // parse object stack
    private Stack<Object> stack = null;
    private ParseState _state = EXPECTING_KEY;

    private ConfigParser(BufferedReader reader, String source)
    {
        this._reader = reader;
        this._file = source;
    }

    /**
     * Take the last-encountered value and place it into the stack of objects as an element of a vector.
     * 
     * @param value
     *            the most recent value encountered - this is an element of a vector we've already begun building.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void assignItem(Object value)
    {
        Object head = head();
        if (head instanceof List)
        {
            // stack looks like this [..., Vector]
            ((List) head).add(value);
            _state = EXPECTING_COMMA;
        }
        else
        {
            throw new ConfigSyntaxException("Unexpected object type on top of stack: " + head.getClass().getName());
        }
    }

    /**
     * Take the last-encountered value and place it into the stack of objects as the value of a map entry.
     * 
     * @param value
     *            the most recent value encountered - this is a value that maps to a key we've already recorded.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void assignValue(Object value)
    {
        Object head = head();
        if (head instanceof String)
        {
            // stack looks like this [..., HashMap, String]
            // if head is a string, then it's a key for a map entry
            String key = (String) pop();

            // the map (or list for top level) is underneath the key
            // and the value was the parameter of this function.
            Object top = head();
            if (top instanceof Map)
            {
                ((Map) top).put(key, value);
            }
            else
            {
                ((List) top).add(new ConfigEntry(key, value));
            }
            // stack looks like this [..., HashMap]
            _state = EXPECTING_SEMICOLON;
        }
        else
        {
            throw new ConfigSyntaxException("Unexpected object type on top of stack: " + head.getClass().getName());
        }
    }

    /**
     * Begin recording a new Array/Vector structure.
     */
    @SuppressWarnings("rawtypes")
    private void beginList()
    {
        // verify first that we want a value or item
        if ((_state != EXPECTING_VALUE) && (_state != EXPECTING_ITEM))
        {
            System.err.println("beginArray -- not expecting value (misplaced '(')");
        }
        // put a new vector into the container atop the stack, also make it the top of the stack.
        Vector vector = new Vector();
        if (head() instanceof String)
        {
            assignValue(vector);
        }
        else if (head() instanceof Vector)
        {
            assignItem(vector);
        }
        else
        {
            throw new ConfigSyntaxException("Unexpected type on top of stack: " + head().getClass().getName());
        }

        this.stack.push(vector);
        // set the state accordingly for a new vector.
        _state = EXPECTING_ITEM;
    }

    /**
     * Begin recording a new Map structure.
     */
    private void beginMap()
    {

        if ((_state != EXPECTING_VALUE) && (_state != EXPECTING_ITEM))
        {
            System.err.println("beginDictionary -- not expecting value (misplaced '{')");
        }
        // place a new map into the container on the top of the stack (map || vector).
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (head() instanceof String)
        {
            assignValue(map);
        }
        else if (head() instanceof Vector)
        {
            assignItem(map);
        }
        else
        {
            throw new ConfigSyntaxException("Unexpected type on top of stack: " + head().getClass().getName());
        }
        this.stack.push(map);

        // set state accordingly for a new map.
        _state = EXPECTING_KEY;
    }

    private StreamTokenizer createStreamTokenizer()
    {
        StreamTokenizer st = new StreamTokenizer(_reader);

        st.resetSyntax();
        // setup the tokenizer
        st.quoteChar('"');
        st.commentChar('#');
        st.eolIsSignificant(false);
        // set whitespace characters to the basic set.
        st.whitespaceChars(' ', ' ');
        st.whitespaceChars('\n', '\n');
        st.whitespaceChars('\t', '\t');
        st.whitespaceChars('\r', '\r');

        st.wordChars('a', 'z');
        st.wordChars('A', 'Z');
        st.wordChars('1', '9');
        st.wordChars('0', '0');

        // add any word chars to this string - these are basically valid
        // characters for unquoted string values.
        String wordChars = "./\\-*&@!\':<>?|[]_^$";
        for (int i = wordChars.length() - 1; i >= 0; --i)
        {
            st.wordChars(wordChars.charAt(i), wordChars.charAt(i));
        }
        return st;
    }

    /**
     * Pop a vector off the top of the stack if the state agrees.
     */
    @SuppressWarnings("rawtypes")
    private void endList()
    {
        // we allow a list to be formed like the following; it makes it easy to
        // comment a line and not worry about placing commas
        /*
         * key = ( value1, value2, value3, );
         */
        if (head() instanceof Vector)
        {
            if ((_state == EXPECTING_ITEM) // saw a comma last, or empty list
                    || ((_state == EXPECTING_COMMA) && (((Vector) head()).size() > 0)))
            {
                // saw an item last, closing the list is OK but size must be positive.
                pop();
            }
        }
        else
        {
            throw new ConfigSyntaxException("Expected Vector on top of stack" + head());
        }
    }

    /**
     * End a Map that is currently on the stack by popping it. Do a sanity check to make sure that the stack does have a map on top.
     */
    private void endMap()
    {
        if ((head() instanceof Map) && (_state == EXPECTING_KEY))
        {
            pop();
        }
        else
        {
            throw new ConfigSyntaxException("Extra '}' or missing ';'");
        }
    }

    /**
     * Parse the file specified at construction into a Map.
     */
    @SuppressWarnings({ "unchecked", "fallthrough" })
    private List<ConfigEntry> getConfigEntries() throws java.io.FileNotFoundException, java.io.IOException
    {
        this.stack = new Stack<Object>();
        this.stack.push(new ArrayList<Object>());
        // push on a new list for the file - a config file maps key ->value.

        StreamTokenizer st = createStreamTokenizer();

        // the main parsing loop - handle special characters - {}()=+;,
        // and string tokens.
        while (st.nextToken() != StreamTokenizer.TT_EOF)
        {
            try
            {
                switch (st.ttype)
                {
                    case StreamTokenizer.TT_WORD:
                    case '"':
                        handleToken(st.sval);
                        break;
                    case ',':
                    {
                        if (_state != EXPECTING_COMMA)
                        {
                            throw new ConfigSyntaxException("',' encountered when not expcted");
                        }
                        _state = EXPECTING_ITEM;
                        break;
                    }
                    case '+':
                    {
                        if (_state != EXPECTING_EQUALS)
                        {
                            throw new ConfigSyntaxException("'+' encountered when not expected");
                        }
                        if (st.nextToken() != '=')
                        {
                            throw new ConfigSyntaxException("'+ must be used only as part of '+='");
                        }
                        String key = (String) this.stack.pop();
                        key += "+";
                        this.stack.push(key);

                    }
                    case '=':
                    {
                        if (_state != EXPECTING_EQUALS)
                        {
                            throw new ConfigSyntaxException("'=' encountered when not expected");
                        }
                        _state = EXPECTING_VALUE;
                        break;
                    }
                    case '{':
                        beginMap();
                        break;
                    case '}':
                        endMap();
                        break;
                    case '(':
                        beginList();
                        break;
                    case ')':
                        endList();
                        break;
                    case ';':
                    {
                        if (_state != EXPECTING_SEMICOLON)
                        {
                            throw new ConfigSyntaxException("';' encountered when not expected");
                        }
                        _state = EXPECTING_KEY;
                        break;
                    }
                    default:
                        throw new ConfigSyntaxException("Unexpected token in parsing: " + st.sval);
                }
            }
            catch (Exception e)
            {
                String msg;
                if (_file != null)
                {
                    msg = "In file " + _file + ": parse error at line number " + st.lineno() + ":(state=";
                }
                else
                {
                    msg = "Parse error in command-line or system property value :(state=";
                }
                msg += _state + ",ttype=" + st.ttype + ",st=[" + st.toString() + "])" + e.getMessage();
                IOException ee = new java.io.IOException(msg);
                ee.initCause(e);
                throw ee;
            }
        }
        // when we run out of file to parse, we should want a key and have one element on the stack
        // (a single List).
        if ((_state != EXPECTING_KEY) || (this.stack.size() != 1))
        {
            if (_file != null)
            {
                throw new ConfigSyntaxException("In file " + _file
                        + " : improperly formed file. Check to make sure you close any and all dictionaries and/or arrays that you have opened.");
            }
            throw new ConfigSyntaxException("Improperly formed command line or system property value");
        }
        return (List<ConfigEntry>) pop();
    }

    /**
     * Handle the next token, given the token and the current state of the parser.
     * 
     * @param object
     *            the token to handle - string.
     */
    private void handleToken(Object object) throws Exception
    {

        switch (_state)
        {
            // keys are 1/2 of an entry in a hashmap, and are encountered before
            // their respective values; they are always of String type
            case EXPECTING_KEY:
                this.stack.push(object);
                _state = EXPECTING_EQUALS;
                break;
            case EXPECTING_EQUALS:
                throw new Exception("Unhandled state for non-special tokens: WANTS_EQUALS");
                // values are the 2nd 1/2 of the map entry, and may be Vector, String, or Map.
            case EXPECTING_VALUE:
                assignValue(object);
                // assignValue sets state for us.
                break;
            // items are elements of a vector, and may be Vector, String, or Map.
            case EXPECTING_ITEM:
                assignItem(object);
                break;
            case UNDEFINED:
                throw new Exception("handleToken -- undefined state (token='" + object.toString() + "')");
            default:
                throw new Exception("handleToken -- unknown state '" + _state + "' (token='" + object.toString() + "')");
        }
    }

    private Object head()
    {
        return this.stack.peek();
    }

    /**
     * Pop an object off of our stack, setting state according to what becomes the top of the stack.
     */
    private Object pop()
    {
        Object object = this.stack.pop();

        if (this.stack.size() != 0)
        {
            Object head = this.stack.peek();
            if ((head instanceof Map) || ((this.stack.size() == 1) && (head instanceof List)))
            {
                _state = EXPECTING_SEMICOLON;
            }
            else if (head instanceof Vector)
            {
                _state = EXPECTING_COMMA; // but will also settle for ')'
            }
            else
            {
                throw new ConfigSyntaxException("Invalid top of stack after pop(): " + head.getClass().getName());
            }
        }
        return object;
    }
}
