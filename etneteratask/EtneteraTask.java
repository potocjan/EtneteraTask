package etneteratask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EtneteraTask {

    private static final int SLOTS = 100;
    private static Map<Integer, Integer> numberCountByNumbers;
    private static EtneteraClient client;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        client = new EtneteraClient();
        numberCountByNumbers = new HashMap<>();

        List<Integer> tip = new ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            tip.add(0);
        }

        try {
            client.registerUser(SLOTS);
            initialTipping(tip);
            constructFirstTip(tip);

            Set<Integer> certainIndexes = new HashSet<>();
            Set<Integer> uncertainIndexes = new HashSet<>();
            for (int i = 0; i < SLOTS; i++) {
                uncertainIndexes.add(i);
            }
            testPermutations(tip, 0, certainIndexes, uncertainIndexes);
        } catch (TaskCompleteException e) {
            System.out.println("Task complete");
        } finally {
            client.close();
        }
    }

    /**
     * Finds which numbers are there, without position
     *
     * @param tip
     */
    private static void initialTipping(List<Integer> tip) {
        int blackTotal = 0;
        for (int i = 1; i < SLOTS; i++) {
            for (int j = 0; j < SLOTS; j++) {
                tip.set(j, i);
            }
            Evaluation result = client.guess(tip);
            int black = result.getBlack();
            blackTotal += black;
            numberCountByNumbers.put(i, black);
        }
        numberCountByNumbers.put(SLOTS, SLOTS - blackTotal);
    }

    private static void constructFirstTip(List<Integer> tip) {
        int index = 0;
        for (Map.Entry<Integer, Integer> entry : numberCountByNumbers.entrySet()) {
            Integer key = entry.getKey();
            Integer value = entry.getValue();
            for (int i = 0; i < value; i++) {
                tip.set(index++, key);
            }
        }
        Collections.shuffle(tip);
    }

    private static void testPermutations(List<Integer> tip, int blackPreviousTurn, Set<Integer> certainIndexes, Set<Integer> uncertainIndexes) {
        int blackBeforeSwap = client.guess(tip).getBlack();
        List<Pair> pairsToSwap = new ArrayList<>();
        List<Integer> rightIndexesFoundThisTurn = new ArrayList<>();

        List<Pair> pairs = constructPairs(uncertainIndexes);

        swap(pairs, tip);
        int blackAfterSwap = client.guess(tip).getBlack();
        swap(pairs, tip); // swap back

        int blackChange = blackAfterSwap - blackBeforeSwap;
        if (blackChange != 0) {
            testPairSwapping(tip, pairsToSwap, rightIndexesFoundThisTurn, pairs, blackBeforeSwap, blackAfterSwap);
        }

        swap(pairsToSwap, tip);
        for (Integer index : rightIndexesFoundThisTurn) {
            certainIndexes.add(index);
            uncertainIndexes.remove(index);
        }

        int minBlackNextTurn = blackPreviousTurn + rightIndexesFoundThisTurn.size();
        shiftValues(tip, uncertainIndexes);

        testPermutations(tip, minBlackNextTurn, certainIndexes, uncertainIndexes);
    }

    private static List<Pair> constructPairs(Set<Integer> uncertainIndexes) {
        List<Pair> pairs = new ArrayList<>();
        Integer[] indexes = uncertainIndexes.toArray(new Integer[uncertainIndexes.size()]);
        for (int i = 0; i + 1 < indexes.length; i = i + 2) {
            pairs.add(new Pair(indexes[i], indexes[i + 1]));
        }
        return pairs;
    }

    private static void swap(List<Pair> pairs, List<Integer> tip) {
        for (Pair pair : pairs) {
            int leftValue = tip.get(pair.getLeft());
            tip.set(pair.getLeft(), tip.get(pair.getRight()));
            tip.set(pair.getRight(), leftValue);
        }
    }

    public static void shiftValues(List<Integer> tip, Set<Integer> uncertainIndexes) {

        int elementsToShift = uncertainIndexes.size();
        Integer[] uncertainIndexesAsArray = uncertainIndexes.toArray(new Integer[uncertainIndexes.size()]);

        if (elementsToShift < 2) {
            // do nothing, task complete
        } else if (elementsToShift == 2) { // swap
            int firstIndex = uncertainIndexesAsArray[0];
            int secondIndex = uncertainIndexesAsArray[1];
            Pair pair = new Pair(firstIndex, secondIndex);
            List<Pair> listOfPairs = new ArrayList<>();
            listOfPairs.add(pair);
            swap(listOfPairs, tip);
        } else if (elementsToShift < 5) { // shuffle values at uncertain indexes
            ArrayList<Integer> values = new ArrayList<>(elementsToShift);
            for (Integer uncertainIndex : uncertainIndexes) {
                values.add(tip.get(uncertainIndex));
            }
            Collections.shuffle(values);
            for (int i = 0; i < elementsToShift; i++) {
                tip.set(uncertainIndexesAsArray[i], values.get(i));
            }
        } else { // shift values at uncertain indexes by two to the left
            int firstValue = tip.get(uncertainIndexesAsArray[0]);
            int secondValue = tip.get(uncertainIndexesAsArray[1]);
            for (int i = 0; i < elementsToShift - 2; i++) {
                tip.set(uncertainIndexesAsArray[i], tip.get(uncertainIndexesAsArray[i + 2]));
            }
            tip.set(uncertainIndexesAsArray[elementsToShift - 2], firstValue);
            tip.set(uncertainIndexesAsArray[elementsToShift - 1], secondValue);
        }
    }

    private static void testPairSwapping(List<Integer> tip, List<Pair> pairsToSwapAfterTurnEnd, List<Integer> indexesToCertainThisTurn, List<Pair> pairs, int blackBeforeSwap, int blackAfterSwap) {
        if (pairs.size() == 1) { // just one pair, nowhere deeper to go, end of recursion
            int blackChange = blackAfterSwap - blackBeforeSwap;
            Pair pair = pairs.get(0);
            switch (blackChange) {
                case 2:
                    pairsToSwapAfterTurnEnd.add(pair);
                    indexesToCertainThisTurn.add(pair.getLeft());
                    indexesToCertainThisTurn.add(pair.getRight());
                    break;
                case 1:
                    testDifferenceByOne(tip, pair, indexesToCertainThisTurn, blackAfterSwap);
                    pairsToSwapAfterTurnEnd.add(pair);
                    break;
                case 0: // changed nothing                    
                    break;
                case -1:
                    testDifferenceByOne(tip, pair, indexesToCertainThisTurn, blackAfterSwap);
                    break;
                case -2:
                    indexesToCertainThisTurn.add(pair.getLeft());
                    indexesToCertainThisTurn.add(pair.getRight());
                    break;
            }
            return;
        }

        // split the tip in two
        List<Pair> leftPairs = pairs.subList(0, (pairs.size() / 2));
        List<Pair> rightPairs = pairs.subList((pairs.size() / 2), pairs.size());

        swap(leftPairs, tip);
        int black = client.guess(tip).getBlack();
        swap(leftPairs, tip); //swap back

        // test left half separately
        if (blackBeforeSwap - black != 0) {
            testPairSwapping(tip, pairsToSwapAfterTurnEnd, indexesToCertainThisTurn, leftPairs, blackBeforeSwap, black);
        }

        // test right half separately
        if (blackAfterSwap - black != 0) {
            testPairSwapping(tip, pairsToSwapAfterTurnEnd, indexesToCertainThisTurn, rightPairs, blackBeforeSwap, blackBeforeSwap - (black - blackAfterSwap));
        }
    }

    private static void testDifferenceByOne(List<Integer> tip, Pair pair, List<Integer> indexesToCertainThisTurn, int blackAfterSwap) {
        int firstIndex = pair.getLeft();
        int secondIndex = pair.getRight();
        int firstValue = tip.get(firstIndex);
        int secondValue = tip.get(secondIndex);
        
        tip.set(firstIndex, secondValue);
        int blackChangeWithSecondValueAtBothIndexes = client.guess(tip).getBlack() - blackAfterSwap;
        tip.set(firstIndex, firstValue); // return back the change

        if (blackChangeWithSecondValueAtBothIndexes != 0) {
            indexesToCertainThisTurn.add(secondIndex);
        } else {
            indexesToCertainThisTurn.add(firstIndex);
        }
    }
}
