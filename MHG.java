/**
 * Class representing the Monster Huting Game in parralel with the Subscriber class
 * (using Monster Hunting Protocol) to communicate with a server running a Broker.
 * 
 * @author Arthur LOUIS
 * @see Subscriber.java
 */

public class MHG{

    private int nbSensorsTriggeredMax = 0;
    private int[][] gameMatrix = new int[10][10];
    private static final char _A_CHAR = 'A';

    /**
     * Initiates a new Monster Hunting Game grid.
     * Simply uses the init method from the class
     * which resets the values of the integers and 
     * the nbSensorsTriggeredMax field.
     */
    public MHG(){
        this.gameInit();
    }

    /**
     * Reinitiates the game grid by setting all the values
     * (matrix of integers and nbSensorsTriggeredMax) to zero.
     * 
     * @see MHG() constructor.
     */
    public void gameInit(){
        nbSensorsTriggeredMax = 0;

        for(int i = 0; i < 10; i++)
            for(int j = 0; j < 10; j++)
                this.gameMatrix[i][j] = 0;
    }

    /**
     * Computes the information provided by a sensor through stream.
     * 
     * @param sensorInfo is a String contaning the coordinates and radius 
     *  of confidence given by the sensor. (Example : D5:2 indicates a sensor
     * triggered on square D5 with a confidence radius of 2).
     */
    public void computeSensor(String sensorInfo){
        
        // Spliting the sensorInfo String into different variables.
        String[] info = sensorInfo.split(":");
        char[] coordinates = info[0].toCharArray();
        int sensorRadius = Integer.valueOf(info[1]);

        int col;
        
        // If coordinates is different from 2, the only way is information from column 10
        // (Example : D10:1).
        if(coordinates.length != 2){
            // In this case we add the values of the 2 last elements from coordinates to find
            // the integer to represent the column.
            String c1 = String.valueOf(coordinates[1]);
            String c2 = String.valueOf(coordinates[2]);
            col = Integer.parseInt(String.valueOf(c1 + c2)) - 1;
        }
        else{
            // Base case (columns 1 - 9).
            String c = String.valueOf(coordinates[1]);
            col = Integer.parseInt(c) - 1;
        }

        // In the two methods above, 1 is substracted from col (make it more easy to use in the loop later used).

        // To fing the row we need to translate a char into an int : simply use modulo 'A' with 'A' = 65.
        int row = coordinates[0] % _A_CHAR;

        // These 4 variables serve the purpose of bounds in the loop to compute.
        int minRow = row - sensorRadius;
        int maxRow = row + sensorRadius;
        int minCol = col - sensorRadius;
        int maxCol = col + sensorRadius;

        // We now add 1 to each cell value because it is computed once more.
        // We also check if the nbOfSensorsTriggeredMax needs to be updated.

        for(int i = minRow; i <= maxRow; i++){
            for(int j = minCol; j <= maxCol; j++){
                
                // at each step we check if we don't try to acces OutOfBounds memory
                // If the i and j represent something outside the matrix, simply ignone it.
                if(i < 0 || i > 9 || j < 0 || j > 9)
                    continue;
                else{
                    this.gameMatrix[i][j]++;
                    
                    // If the newly updated square has a value over nbSensorsTriggeredMax, update it.
                    if(this.gameMatrix[i][j] > this.nbSensorsTriggeredMax)
                        this.nbSensorsTriggeredMax = this.gameMatrix[i][j];
                }
            }
        }
    }

    /**
     * Method to represent the matrix in the terminal on a nicely shaped grid.
     * 
     * The squares where the monster can be (where the value in the grid equals nbSensorsTriggeredMax)
     * is represented with an 'X'.
     */
    public void printMatrix(){
        String columns = "    1  2  3  4  5  6  7  8  9 10";
        String limits = "   -- -- -- -- -- -- -- -- -- --";
        
        System.out.println(columns);
        System.out.println(limits);

        for(int i = 0; i < 10; i++){
            // Again use 'A' to simply write chars with numbers
            System.out.print((char) (_A_CHAR + i) + " ");
            for(int j = 0; j < 10; j++){
                System.out.print("| ");

                // Checks if value in the grid equals nbSensorsTriggeredMax thus represented by 'X'.
                if(this.gameMatrix[i][j] == this.nbSensorsTriggeredMax){
                    System.out.print("X");
                }
                else{
                    System.out.print(" ");
                }
            }
            System.out.println("|");
            System.out.println(limits);
        }
        System.out.println();
    }
}