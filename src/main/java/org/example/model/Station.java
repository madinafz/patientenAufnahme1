package org.example.model;

public class Station {

   private int id;
        private String name;
        private int maxBetten;

        public Station(int id, String name, int maxBetten) {
            this.id = id;
            this.name = name;
            this.maxBetten = maxBetten;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getMaxBetten() { return maxBetten; }
        public void setMaxBetten(int maxBetten) { this.maxBetten = maxBetten; }


}
