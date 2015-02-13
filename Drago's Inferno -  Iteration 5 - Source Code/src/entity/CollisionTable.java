/*
 * CollisionTable.java (v1.0)
 * 3/17/2013
 */
package entity;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import entity.Entity;

/**
 * The collision table, used as a reference in the collision
 * response routine to decide how two colliding entities should
 * be affected by each other.
 *  
 * @author Quothmar
 *
 */
public class CollisionTable
{
    // Static collision table flags.
    public static final int CT_PASS = 1;
    public static final int CT_SLIDE = 2;
    public static final int CT_DESTROY = 4;
    public static final int CT_REFLECT = 8;
    public static final int CT_REFLECT_TARGET = 16;
    public static final int CT_TAKE = 32;
    public static final int CT_BOUNCE = 64;
    public static final int CT_DISAPPEAR = 128;
    public static final int CT_DAMAGE = 256;
    public static final int CT_NUDGE_TARGET = 512;
    public static final int CT_TAILSTRIKE_TARGET = 1024;
    public static final int CT_STOP = 2048;
    public static final int CT_GET_HURT = 4096;
    public static final int CT_SHATTER = 8192;
    public static final int CT_GET_FROZEN = 16384;
    public static final int CT_GET_BURNED = 32768;
    public static final int CT_BURN = 65536;
    public static final int getFlagFromString(String s) {
        switch (s) {
        case "PASS": return CT_PASS;
        case "SLIDE": return CT_SLIDE;
        case "DESTROY": return CT_DESTROY;
        case "REFLECT": return CT_REFLECT;
        case "REFLECT_TARGET": return CT_REFLECT_TARGET;
        case "TAKE": return CT_TAKE;
        case "BOUNCE": return CT_BOUNCE;
        case "DISAPPEAR": return CT_DISAPPEAR;
        case "DAMAGE": return CT_DAMAGE;
        case "NUDGE_TARGET": return CT_NUDGE_TARGET;
        case "TAILSTRIKE_TARGET": return CT_TAILSTRIKE_TARGET;
        case "STOP": return CT_STOP;
        case "GET_HURT": return CT_GET_HURT;
        case "SHATTER": return CT_SHATTER;
        case "GET_FROZEN": return CT_GET_FROZEN;
        case "GET_BURNED": return CT_GET_BURNED;
        case "BURN": return CT_BURN;
        default: return 0;
        }
    }
    
    // Static collision table arrays.
    public static int flags[][];
    public static int damageGiven[][];
    public static int damageTaken[][];
    public static int bounce[][];
    
    // Static method loads the Collision Table.
    public static void load() {
        
        final int numstates = Entity.currentNumStates;
        
        // Load collision table from Excel file saved as 'tab-delimited'.
        Scanner scanner1 = null;
        Scanner scanner2 = null;
        Scanner scanner3 = null;
        String filename = "Collision Table - Text Formatted.txt";
        try {
            scanner1 = new Scanner(new File(filename));
            scanner2 = new Scanner(new File(filename));
            scanner3 = new Scanner(new File(filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        // Get height and width of collision table.
        int tableWidth = 0;
        int tableHeight = 0;
        scanner1.useDelimiter("\n");
        while (scanner1.hasNext()) { scanner1.next(); ++tableHeight; }
        tableHeight -= 2;
        scanner2.useDelimiter("\t");
        while (scanner2.hasNext()) { scanner2.next(); ++tableWidth; }
        tableWidth /= tableHeight;
        tableWidth -= 1;
        
        // Use tab as the delimiter.
        scanner3.useDelimiter("\t");
        
        // Define variables and initialize game's collision table.
        int[] columnState = new int[tableWidth];
        int[] rowState    = new int[tableHeight];
        String[][] textEntries = new String[numstates][numstates];
        flags       = new int[numstates][numstates];
        damageGiven = new int[numstates][numstates];
        damageTaken = new int[numstates][numstates];
        bounce      = new int[numstates][numstates];
        int column = 0;
        int row    = 0;
        
        // Go to the first header state.
        String entry;
        while (true) {
            entry = scanner3.next();
            if (entry.startsWith("ST_")) break;
        }

        // Get all the header states.
        while (true) {
            columnState[column] = Entity.getStateByString(entry);
            ++column;
            if (column == tableWidth) break;
            entry = scanner3.next();
        }
        
        // Load the collision table entries as strings.
        for (row = 0; row < tableHeight; ++row) {
            
            // Get the state for the next row.
            String rowHeader = scanner3.next();
            rowHeader = rowHeader.substring(2);
            rowState[row] = Entity.getStateByString(rowHeader);
            
            // Get all the entries for this row, putting them this time in
            //   numerical order by state rather than by Excel column/row.
            for (column = 0; column < tableWidth; ++column)
               textEntries[columnState[column]][rowState[row]] = scanner3.next();
            
        }
        
        // Parse each string entry of the text-based collision table to
        //   obtain actual values of the game's collision table.
        String flag;
        for (row = 0; row < numstates; ++row) {
            for (column = 0; column < numstates; ++column) {
                
                // Get a scanner for the next text-based table entry.
                if (textEntries[column][row] == null) continue;
                scanner1 = new Scanner(textEntries[column][row]);
                
                // Parse by comma.
                scanner1.useDelimiter(",");
                
                // Set flags and values for this entry.
                while (scanner1.hasNext()) {
                    
                    // Get the next flag in this entry, removing quotation
                    //   marks as needed, as Excel sometimes adds them.
                    flag = scanner1.next();
                    if (flag.startsWith("\"")) flag = flag.substring(1);
                    if (flag.endsWith("\"")) flag = flag.substring(0, flag.length() - 1);
                    
                    // Get any value attached to this flag, e.g., '30' of
                    //   DAMAGE(30), and trim string to equal the flag.
                    int value = 0;
                    if (flag.contains("(")) {
                        if (!flag.contains(")")) error("load", "missing closing parenthesis");
                        String s = flag.substring(flag.indexOf("(") + 1, flag.indexOf(")")); 
                        value = Integer.parseInt(s);
                        flag = flag.substring(0, flag.indexOf("("));
                    }

                    // Determine what flag this is, and set the corresponding flag
                    //   in the game's collision table. Also add any values.
                    flags[column][row] |= getFlagFromString(flag);
                    switch(getFlagFromString(flag)) {
                    case CT_DAMAGE:     damageGiven[column][row] += value;  break; 
                    case CT_GET_HURT:   damageTaken[column][row] += value;  break;
                    case CT_BOUNCE:     bounce[column][row] += value;       break;
                    }

                } // end while (by flag of entry)
            } // end for (by column)
        } // end for (by row)
    } // end method load

    private static void error(String function, String message) {
        System.out.println("CollisionTable." + function + "(): " + message);
        System.exit(1);
    }

} // end class CollisionTable
