import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.HashMap;

/**
 * Creating an enum for the different types of events.
 * 
 * @author Brian Zhou
 */
enum EventTypeEnum {
    PLAYER_CHOICE,
    RANDOM,
}



/**
 * An abstract classes that represent events in the story parsed by Parse.java.
 * 
 * @author Brian Zhou
 */
abstract class Event {
    // values representing the type of an event
    final static EventTypeEnum RANDOM = EventTypeEnum.RANDOM;
    final static EventTypeEnum PLAYER_CHOICE = EventTypeEnum.PLAYER_CHOICE;

    // fields stored in the event
    protected String name;
    protected String text;
    protected LinkedList<String> options = new LinkedList<String>();
    protected EventTypeEnum type;

    // methods which will be overwritten:
    // (if accessed incorrectly it will return the default values)
    public String findOption(String val) {
        return "";
    }

    public String findOption(Double val) {
        return "";
    }

    public boolean isOption(String str) {
        return true;
    }

    // getters:
    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    public LinkedList<String> getOptions() {
        return options;
    }

    public EventTypeEnum getType() {
        return this.type;
    }

}

/**
 * A class which represents an event which requires user input.
 */
class PlayerChoiceEvent extends Event {
    // constants for the input from the parser

    private final static int VALUE_i = 0;
    private final static int ACCEPTED_VALUE_i = 1;
    private final static int EVENT_NAME_i = 2;

    // hashmap for options
    private HashMap<String, String> optionsMap = new HashMap<String, String>();

    /**
     * A constructor for PlayerChoiceEvent.
     */
    public PlayerChoiceEvent(String eventName, String eventText, LinkedList<String[]> options) {
        // storing values
        this.name = eventName;
        this.text = eventText;
        this.type = PLAYER_CHOICE;

        // adding each option in the correct format to both optionsMap and option
        for (String[] option : options) {
            this.optionsMap.put(option[ACCEPTED_VALUE_i], option[EVENT_NAME_i]);
            this.options.add(String.format("%s (Enter \"%s\" to select this option)", option[VALUE_i],
                    option[ACCEPTED_VALUE_i]));
        }
    }

    /**
     * A method to search for a option in the optionsMap. This method will return
     * null if vals is not an option.
     */
    public String findOption(String str) {
        return optionsMap.get(str);
    }

    /**
     * A method to identify wether a value is an option.
     */
    public boolean isOption(String str) {
        return optionsMap.containsKey(str);
    }
}

/**
 * A class which represents an event which takes random input.
 */
class RandomEvent extends Event {
    // constants for the input from the parser
    private final static int PERCENT_i = 0;
    private final static int EVENT_NAME_i = 1;

    // hashmap for options
    private HashMap<Integer, String> optionsMap = new HashMap<Integer, String>();

    /**
     * Constructor for RandomEvent.
     */
    public RandomEvent(String eventName, String eventText, LinkedList<String[]> options) {
        // storing values
        this.name = eventName;
        this.text = eventText;
        this.type = RANDOM;

        // adding all options to optionsMap and options
        int curr_percent = 0;
        for (String[] option : options) {
            curr_percent += Integer.parseInt(option[PERCENT_i]);
            this.optionsMap.put(curr_percent, option[EVENT_NAME_i]);
        }
    }

    /**
     * A method to search for a option in the optionsMap. This method will return
     * null if vals is not an option.
     */
    public String findOption(Double randVal) {
        int min_gt_val = 100;
        for (int key : this.optionsMap.keySet()) {
            if (key > randVal && key < min_gt_val) {
                min_gt_val = key;
            }
        }

        return this.optionsMap.get(min_gt_val);
    }
}

/**
 * Creates a class to parse the inputted file to separate out the events
 * detailed in it.
 * 
 * @author Brian Zhou
 */
class Parse {
    /**
     * Parses the file at path, returning a linked list of the events represented in
     * the file.
     * 
     * @param path the path to the file storing the story
     * @return a linked list of events
     * @throws FileNotFoundException
     */
    public static LinkedList<Event> parse(String path) throws FileNotFoundException {
        // reading from the file
        File file = new File(path);
        Scanner reader = new Scanner(file);

        // creating empty list to store events
        LinkedList<Event> events = new LinkedList<Event>();

        // markers
        boolean inEvent = false;
        boolean inComment = false;
        boolean isPlayerChoice = false;

        // values needed to create a new event
        String eventName = "";
        String eventText = "";
        LinkedList<String[]> currOptions = new LinkedList<String[]>();

        // reading the file:
        while (reader.hasNextLine()) {

            // reading the next line
            String line = reader.nextLine();

            // skip empty lines
            if (line.length() == 0) {
                continue;
            }

            // start of comment
            if (startsWith(line, "/*")) { // line.length() >= 2 && line.substring(0, 2).equals("/*")
                inComment = true;
                continue;
            }

            // end of comment
            if (startsWith(line, "*/")) { // line.length() >= 2 && line.substring(0, 2).equals("*/"
                inComment = false;
                continue;
            }

            // skipping everything in a comment
            if (inComment) {
                continue;
            }

            // start of event
            if (startsWith(line, "{")) {
                eventName = line.substring(1);
                inEvent = true;
                continue;
            }

            // end of event
            if (startsWith(line, "}")) {
                // adding new event
                Event event;
                if (isPlayerChoice) {
                    event = new PlayerChoiceEvent(eventName, eventText, currOptions);
                } else {
                    event = new RandomEvent(eventName, eventText, currOptions);
                }
                events.add(event);

                // resetting modified values
                eventName = "";
                eventText = "";
                currOptions.clear();
                inEvent = false;
                continue;
            }

            // not checking for options if currently not inside of an event
            if (!inEvent) {
                continue;
            }

            // player choice options
            if (startsWith(line, "    - ")) {
                // marking this event is a player choice one
                isPlayerChoice = true;

                // adding the option on the current line
                currOptions.add(line.substring(6).split(";; "));
                continue;
            }

            // random choice options
            if (startsWith(line, "    %")) {
                // marking this event is a random one
                isPlayerChoice = false;

                // adding the outcome on the current line
                currOptions.add(line.substring(5).split(";; "));
                continue;
            }

            // event text
            if (startsWith(line, "- ")) {
                eventText = line.substring(2);
                continue;
            }
        }

        // closing the scanner
        reader.close();

        return events;
    }

    /**
     * Finds if a line starts with value.
     */
    public static boolean startsWith(String line, String value) {
        return line.length() >= value.length() && line.substring(0, value.length()).equals(value);
    }
}



/**
 * The starting point for the program.
 * 
 * @author Brian Zhou
 */
public class Main {
  /**
   * Brian Zhou
   * 8/29/23
   * 5th Period
   * Choose your own adventure
   * 
   * You wake up for a normal seaming day of school, with only one small problem:
   * it's the apocalypse.
   */

  // constants
  final static EventTypeEnum RANDOM = EventTypeEnum.RANDOM;
  final static EventTypeEnum PLAYER_CHOICE = EventTypeEnum.PLAYER_CHOICE;
  final static String START = "#START";
  final static String END = "#END";
  final static int SLEEP_AMOUNT = 1000; // amount in milliseconds

  // driver code
  public static void main(String[] args) throws InterruptedException {
    LinkedList<Event> events;

    // find the path from where java starts to check to the directory containing the
    // current one
    File directory = new File("./");

    // replace the ending "/." with the path to story file
    final String PATH = directory.getAbsolutePath()
        .replace(".", "/story.txt");

    // get all events in a LinkedList
    try {
      events = Parse.parse(PATH);
    } catch (FileNotFoundException e) { // the file path was not correct
      System.out.printf("\"%s\" was not found\n", PATH);
      return;
    }

    // searching for the starting event
    Event currEvent = searchEvents(events, START);

    // creating a scanner
    Scanner scanner = new Scanner(System.in);

    // looping until the end of the story is reached:
    while (!currEvent.getName().equals(END)) {
      // prompt the player with the event
      printPrompt(currEvent);

      // receive input from player
      String nextEvent = verifiedInput(currEvent, scanner);

      Thread.sleep(SLEEP_AMOUNT);

      // find next event to prompt player
      currEvent = searchEvents(events, nextEvent);
    }

    // print the text of the ending event
    System.out.println(currEvent.getText());
    scanner.close(); // free up resources
  }

  /**
   * This method takes input from the player and returns the next event if it
   * input correctly selects an option; otherwise, the method will re-prompt and
   * take input from player until an option is correctly selected.
   */
  public static String verifiedInput(Event event, Scanner scanner) {
    // retry until input is correct:
    while (true) {
      try {
        double doubleInput = 0.0;
        String strInput = "";

        // get input
        if (event.getType() == RANDOM) {
          doubleInput = Math.random() * 100;
        } else {
          strInput = scanner.nextLine();

          // checking that the player inputted a correct option
          if (!event.isOption(strInput)) {
            throw new IllegalArgumentException();
          }
        }

        // separate out the input of the player from the next prompt
        System.out.println();

        String nextEvent;
        // search for the option, this will throw IllegalArgumentException if the option
        // is not found
        if (event.getType() == RANDOM) {
          nextEvent = event.findOption(doubleInput);
        } else {
          nextEvent = event.findOption(strInput);
        }

        // if the option was found, return it
        return nextEvent;
      }
      // if the option is not found:
      catch (IllegalArgumentException e) {
        // re-prompt the player
        System.out.println("Sorry, your input was not accepted, please try again.");
        printPrompt(event);
      }
    }
  }

  /**
   * Searches a LinkedList of events for a target name.
   * 
   * @throws IllegalArgumentException if an event of this name is not found
   */
  public static Event searchEvents(LinkedList<Event> events, String targetName) throws IllegalArgumentException {
    // searching and returning if an event of the target name was found
    for (Event event : events) {
      if (event.getName().equals(targetName)) {
        return event;
      }
    }

    // throw IllegalArgumentException if an event of the target name was not found
    throw new IllegalArgumentException();
  }

  /**
   * Prints a prompt using an event.
   */
  public static void printPrompt(Event event) {
    // prints the text of the event
    System.out.println(event.getText());

    // if the event asks for random input, do not print it's options
    if (event.getType() == RANDOM) {
      return;
    }

    // print the options for this event
    for (String option : event.getOptions()) {
      System.out.println("\t- " + option);
    }
    System.out.print(">>>");
  }
}
