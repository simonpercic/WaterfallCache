Change Log
==========

Version 1.1.0 *(20??-??-??)*
----------------------------

 * New: Replaced Reservoir disk cache with [Bucket](https://github.com/simonpercic/Bucket).
 * New: Added async methods to WaterfallCache.
 * New: Passing `Type` instead of `Class<T>` to properly support Java generics and collections.
 * New: Added the ability to specify which thread to subscribe to results on (via ObserveOn Rx Scheduler).
 * New: A lot of tests.
 * Fix: No more silent failures in get and contains data pre-fetching.



Version 1.0.2 *(2015-09-12)*
----------------------------

 * New: Ability to use a custom `SimpleTimeProvider` or an `Observable` time.
 * New: Added the [Soter plugin](https://github.com/dlabs/soter).


Version 1.0.1 *(2015-08-05)*
----------------------------

 * New: Updated Retrolambda plugin to 3.2.0.


Version 1.0.0 *(2015-08-04)*
----------------------------

Initial release.
