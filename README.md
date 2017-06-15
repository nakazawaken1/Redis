# Redis client
Simple Redis client

version 1.1.0 Simplify API, Add Type, Reply
version 1.0.0 First release

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
	      <version>1.1.0</version>
	    </dependency>
	  </dependencies>
	</project>

[Example]

    public static void main(String[] args) throws Exception {
        PrintStream out = System.out;
        try (Redis redis = new Redis()) {
            String[] keys = { "KEYS", "*" };

            // Enumerate keys
            out.println("< " + String.join(" ", keys));
            out.println("> " + redis.command(keys));
            out.println();

            // set value
            String[] set = { "SET", "a", "テスト" };
            out.println("< " + String.join(" ", set));
            out.println("> " + redis.command(set));
            out.println();

            // set value
            String[] set2 = { "SET", "b", "あいう\nえお" };
            out.println("< " + String.join(" ", set2));
            out.println("> " + redis.command(set2));
            out.println();

            // get value
            String[] get = { "GET", "a" };
            out.println("< " + String.join(" ", get));
            out.println("> " + redis.command(get));
            out.println();

            // get value
            String[] get2 = { "GET", "b" };
            out.println("< " + String.join(" ", get2));
            out.println("> " + redis.command(get2));
            out.println();

            // Enumerate keys
            out.println("< " + String.join(" ", keys));
            out.println("> " + redis.command(keys));
            out.println();

            // delete value
            String[] del = { "DEL", "a" };
            out.println("< " + String.join(" ", del));
            out.println("> " + redis.command(del));
            out.println();

            // Enumerate keys
            out.println("< " + String.join(" ", keys));
            out.println("> " + redis.command(keys));
            out.println();

            // delete value
            String[] del2 = { "DEL", "b" };
            out.println("< " + String.join(" ", del2));
            out.println("> " + redis.command(del2));
            out.println();

            // Enumerate keys
            out.println("< " + String.join(" ", keys));
            out.println("> " + redis.command(keys));
            out.println();
        }
    }

(Result)

	< KEYS *
	> 0 items
	
	< SET a テスト
	> +OK
	
	< SET b あいう
	えお
	> +OK
	
	< GET a
	> $テスト
	
	< GET b
	> $あいう
	えお
	
	< KEYS *
	> 2 items
	[1] $a
	[2] $b
	
	< DEL a
	> :1
	
	< KEYS *
	> 1 items
	[1] $b
	
	< DEL b
	> :1
	
	< KEYS *
	> 0 items
