# AtWuuBernstein
A twitter-like distributed log.
"
The distributed system consists of N sites, each site corresponding to a single user. Every user “follows” all users. Initially, a follower should see all tweets from the users he or she follows. A user can “block” a follower; when that follower is blocked, the follower should not be able to view any of the user’s tweets (old or new). A user can also “unblock” a blocked follower, which once again allows the follower to view all of the user’s tweets.
A tweet is a tuple, consisting of the following fields:
• User: creator of the tweet
• Message: the text of the tweet
• Time: from the physical clock of the node where the tweet was created (you must account
for different time zones).
...
Each user can execute the following operations:
1. tweet<message>:createsanewtweet
2. view : displays the timeline, i.e., the entire set of tweets (including all fields), sorted in
descending order by the time field (most recent tweets appear first), excluding tweets that
the user is blocked from seeing.
3. block <username> : blocks the specified username from viewing tweets of the user
who issued the command.
4. unblock <username> : unblocks the specified username; allows the viewing of tweets of
the user who issued the command.
...
This implementation uses a configuration file that gives the IP addresses and ports that will be used for communication between all sites.
...
A correct implementation of the algorithm will ensure that when a user views a tweet T in his or her timeline, the user also views all tweets that causally precede T that he has permission to view (is not blocked from viewing).
"
from http://www.cs.rpi.edu/~pattes3/dsa/DSA_Project1.pdf