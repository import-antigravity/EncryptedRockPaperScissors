import java.util.regex.Pattern;

/**
 * Simple class that contains some logic for rock-paper-scissors.
 * @author grrdozier
 */
public class Player {
    /**
     * 0: idle
     * 1: rock
     * 2: paper
     * 3: scissors
     *
     * 3 - 2 = 1
     * 2 - 1 = 1
     * 1 - 3 = -2
     */
    private int move;

    public Player(){
        move = 0;
    }

    public int getMove() {
        return move;
    }

    public void setMove(int move) {
        this.move = move;
    }

    /**
     * Takes in a string and returns the number corresponding to that move
     *
     * @param msg a string of the form "r, p, s" or "rock, paper, scissors", ignoring case
     * @return the number corresponding to that move
     * @throws IllegalArgumentException if the input string is not valid
     */
    public static int getMoveFromString(String msg) throws IllegalArgumentException {
        if (Pattern.compile("r(ock)?").matcher(msg.toLowerCase()).matches())
            return 1;
        else if (Pattern.compile("p(aper)?").matcher(msg.toLowerCase()).matches())
            return 2;
        else if (Pattern.compile("s(cissors)?").matcher(msg.toLowerCase()).matches())
            return 3;
        else throw new IllegalArgumentException();
    }

    /**
     * Takes two {@code Player} instances and returns the winner. Assumes that both players' moves are 1, 2, or 3.
     *
     * @param player1 First player
     * @param player2 Second player
     * @return The player with the winning move
     */
    public static Player getWinner(Player player1, Player player2) {
        if (player1.getMove() == player2.getMove())
            return null;
        else if (player1.getMove() - player2.getMove() == 1 || player1.getMove() - player2.getMove() == -2)
            return player1;
        else
            return player2;
    }
}
