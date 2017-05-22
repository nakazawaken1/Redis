# Redis client
Simple Redis client

[Maven usage]

	<project>
	  ...
	  <repositories>
	    <repository>
	      <id>qpg.jp</id>
	      <name>qpg.jp repository</name>
	      <url>http://qpg.jp/maven</url>
	    </repository>
	  </repositories>
	  <dependencies>
	    <dependency>
	      <groupId>jp.qpg</groupId>
	      <artifactId>Redis</artifactId>
	      <version>1.0.0</version>
	    </dependency>
	  </dependencies>
	</project>

[Example]

    try (Redis redis = new Redis("127.0.0.1", 6379)) {
	
        // set value
        redis.command("SET", "a", "テスト");
	
        // get value
        redis.command("GET", "a");
        System.out.println(redis.response().getValue());
	
        // set value
        redis.command("SET", "b", "Hello!");
	
        // Enumerate keys
        redis.command("KEYS", "*");
        long n = redis.readLong('*');
        for (long i = 1; i <= n; i++) {
            System.out.println(i + ") " + redis.responseText());
        }
	
        // delete value
        redis.command("DEL", "a");
    }
