package etneteratask;

public class Evaluation {
    private final int black;
    private final int white;

    public Evaluation(int black, int white) {
        this.black = black;
        this.white = white;
    }

    public int getBlack() {
        return black;
    }

    public int getWhite() {
        return white;
    }

    @Override
    public String toString() {
        return "Evaluation{" + "black=" + black + ", white=" + white + '}';
    }
}
