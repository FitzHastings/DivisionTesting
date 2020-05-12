package atolincore5;

public class Connection
    {
        private transient String name = iDevice.NAME;
        private transient String host = "127.0.0.1";
        private transient int port = 1099;
        
        public Connection() { }
        
        public Connection(String name, String host, int port)
        {
            this.name = name;
            this.host = host;
            this.port = port;
        }
        
        public String getName()
        {
            return name;
        }
        
        public void setName(String name)
        {
            this.name = name;
        }
        
        public String getHost()
        {
            return host;
        }
        
        public void setHost(String host)
        {
            this.host = host;
        }
        
        public int getPort()
        {
            return port;
        }
        
        public void setPort(int port)
        {
            this.port = port;
        }
    }
