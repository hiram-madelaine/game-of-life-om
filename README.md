# An Om/React client of the game of life

Based on cgrand's [solution] (https://github.com/clojurebook/ClojureProgramming/tree/master/ch03-game-of-life) in the book Clojure Programming

The visualisation is inspired by the excellent presentation of React.js by [steveluscher at react-supervanjs-2013] (https://github.com/steveluscher/react-supervanjs-2013)

The purpose is to help people discover and understand Om/React during a dojo at the Paris Clojure Meetup.


## Getting starter

Based on David Nolen's template : mies-om

`lein cljsbuild auto`


Open the project with LightTable

You are presented en empty world, free of any cell.


You can create new cells by the mouse actions :

`click, hover, click`

The second click will add the new cells to the world.


It is possible to create non contiguous cells by pressing `esc` key before the second `click`.


Click on "Wipe board" button to kill all living cells and new ones.


Click on the "Mark" button to color each location with direct DOM manipulation.
