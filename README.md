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

    public static void main(String[] args) throws Exception {
        PrintStream out = System.out;
        try (Redis redis = new Redis()) {
            Runnable keys = () -> {
                try {
                    String[] command = { "KEYS", "*" };
                    out.println("< " + String.join(" ", command));
                    redis.command(command);
                    long n = redis.readLong('*');
                    out.println("> :" + n);
                    for (int i = 1; i <= n; i++) {
                        out.println("> " + i + ") " + redis.responseText());
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
            // Enumerate keys
            keys.run();

            // set value
            String[] set = { "SET", "a", "テスト" };
            out.println("< " + String.join(" ", set));
            redis.command(set);
            out.println("> " + redis.responseText());

            // get value
            String[] get = { "GET", "a" };
            out.println("< " + String.join(" ", get));
            redis.command(get);
            out.println("> " + redis.responseText());

            // Enumerate keys
            keys.run();

            // delete value
            String[] del = { "DEL", "a" };
            out.println("< " + String.join(" ", del));
            redis.command(del);
            out.println("> " + redis.responseText());

            // Enumerate keys
            keys.run();
        }
    }

(Result)

	< KEYS *
	> :0
	< SET a テスト
	> +OK
	< GET a
	> $テスト
	< KEYS *
	> :1
	> 1) $a
	< DEL a
	> :1
	< KEYS *
	> :0
