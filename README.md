# EtneteraTask
Etnetera mastermind puzzle solution

## Algorithm
Based on swapping pairs. We get the number of black pins before and after swap. If there is a change in the number of pins, we investigate further and find out which numbers belong to which positions.

### Details
At first we find, how many of which numbers are there (since they can repeat).
Then we test relevant permutations in turns.

At the beginning of each turn, we know which numbers at which positions we know (first turn we know nothing). Then we create pairs of uncertain numbers. We test how many black pins we get before swap, and after swap of all pairs. If there is no change, end of turn. Otherwise we divide the pairs in two groups and apply the algorithm recursively.

At the end of each turn, we know if we found any numbers (and positions they are at), so we add them to the set of known values. Then shuffle / shift uncertain values and continue in next turn.

When we guessed the right solution, an exception is thrown and catched in the main function.

The algorithm could be even faster, if the initial tipping didn't take 99 guesses. 

In the solution, we only work with black pins.
