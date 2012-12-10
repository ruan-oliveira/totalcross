/*********************************************************************************
 *  TotalCross Software Development Kit - Litebase                               *
 *  Copyright (C) 2000-2012 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *********************************************************************************/

package litebase;

/**
 * Hash table for Litebase SQL reserved words.
 */
class ReservedHashtable
{
   /** 
    * The hash table data. 
    */
   private Entry[] table;

   /**
   * Constructs a new, empty hash table with the specified initial capacity.
   *
   * @param initialCapacity The number of elements you think the hash table will end with. 
   */
   ReservedHashtable(int initialCapacity)
   {
      table = new Entry[initialCapacity];
   }

   /**
   * Returns the value to which the specified hash is mapped in this hash table. 
   * That is, return the token code if a reserved word is passed as a key which matches the identifier found.
   *
   * @param hash The key hash in the hash table.
   * @param string The identifier with the passed hash code inside a <code>StringBuffer</code>.
   * @return The value to which the key is mapped in this hash table; -1 if the key is not mapped to any value in this hash table.
   */
   int get(int hash, StringBuffer string)
   {
      int index = (hash & 0x7FFFFFFF) % table.length;
      for (Entry e = table[index] ; e != null ; e = e.next)
         if (e.hash == hash && equalsSB((String)e.key, string))
            return e.value;
      return -1;
   }

   /**
   * Maps the specified key to the specified value in this hash table. That is, puts the pair <reserved word, token code> in the hash table.
   *
   * @param key The hash table key.
   * @param value The value.
   */
   void put(Object key, int value)
   {
      Entry[] tab = table;
      int hash = key.hashCode(), // flsobral@tc100b4_23: this operation throws NPE if key is null, no need to explicitly test that.
          index = (hash & 0x7FFFFFFF) % tab.length;

      // Creates the new entry.
      Entry e = new Entry();
      e.hash = hash;
      e.key = key;
      e.value = value;
      e.next = tab[index];
      tab[index] = e;
   }
   
   /**
    * Compares the contents of a <code>String</code> and a <code>StringBuffer</code>. 
    * 
    * @param string The <code>String</code>.
    * @param sBuffer The <code>StringBuffer</code>. 
    * @return <code>true</code> if the contents are the same; <code>false</code>, otherwise.
    */
   private boolean equalsSB(String string, StringBuffer sBuffer)
   {
      int i = string.length();
      
      if (i != sBuffer.length())
         return false;
      
      while (--i >= 0)
         if (string.charAt(i) != sBuffer.charAt(i))
            return false;
      return true;
   }
}