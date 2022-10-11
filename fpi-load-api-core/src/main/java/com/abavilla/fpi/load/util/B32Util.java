/******************************************************************************
 * FPI Application - Abavilla                                                 *
 * Copyright (C) 2022  Vince Jerald Villamora                                 *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.     *
 ******************************************************************************/

package com.abavilla.fpi.load.util;

import java.io.IOException;

/**
 * A simple Base32 encoder and decoder based on the character set proposed by Douglas Crockford.
 *
 * @author D.Fichtmueller
 * @author <a href="mailto:vincevillamora@gmail.com">Vince Villamora</a>
 * @see <a href="http://www.crockford.com/wrmg/base32.html">Base32 Encoding by Douglas Crockford</a>
 */
public abstract class B32Util {
  private static final char[] CHARACTER_TABLE = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M',
      'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z',
      '*', '~', '$', '=', 'U',
  };

  /**
   * Encodes a non negative long into a Crockford Base32 String.
   *
   * @param number the non negative number to encode. An IllegalArgumentException is thrown when the number is negative.
   * @param withCheckChar a boolean indicating whether the last character of the encoded string should be a checksum character.
   * @param length the minimum length of the resulting base32 string. If the result is shorter then additional zeros are added to beginning of the result string. This parameter will be ignored if it is smaller then the resulting string, including it being zero or negative.
   * @param blockWidth the number of characters after which a dash (-) is introduced for better readability. The block width is ignored if it is zero or negative.
   * @return a Crockford base32 string which represents the number given as parameter
   */
  public static String encode(long number, boolean withCheckChar, int length, int blockWidth){
    //check if number is not negative
    if(number<0){
      throw new IllegalArgumentException("The number parameter can not be negative.");
    }

    StringBuilder response = new StringBuilder();
    //calculate the checksum already, even if it is not required, as the number will be changed during the conversion
    int checksum = (int)(number % 37);

    do{
      //convert the number into the string, by calculating the module 32 of the current number,
      //looking up the corresponding encoding symbol for this value and putting it in front of the already existing result string
      int remainder = (int)(number % 32);
      response.insert(0, CHARACTER_TABLE[remainder]);

      //calculate the next higher digit by dividing by 32 (long division without fractions)
      number = number/32;
    }while(number > 0);

    if(withCheckChar){
      response.append(CHARACTER_TABLE[checksum]);
    }

    //add padding zeros to the beginning if the result string length is shorter than the length parameter
    if(length>0){
      while(response.length()<length){
        response.insert(0, "0");
      }
    }

    //insert spaces for better readability if the block width is given
    if(blockWidth>0){
      StringBuilder spacedResponse = new StringBuilder();
      //copy the response character by character
      for(int i=0;i<response.length();i++){
        //if the current character position is a pure multiple of blockWidth but not at the first character (i=0), then add a dash
        if(i%blockWidth == 0 && i > 0){
          spacedResponse.append("-");
        }
        //add the character at the current position
        spacedResponse.append(response.substring(i, i+1));
      }
      response = spacedResponse;
    }

    return response.toString();
  }

  /**
   * Decodes a Crowford Base32 String into a number.
   *
   * <p>This implementation deviates from Crowford's Base32 Specification in the fact that the character 'U' (or lowercase 'u') is considered as a variant of the letter V when it is not used as the check symbol.
   * 	This makes the implementation less strict then the specification and handles potential misreadings of the letter V</p>
   *
   *
   * @param string The string to be decoded.
   * 			If the string contains characters that are not allowed within a Crowford Base32 String, an IllegalArgumentException is thrown.
   * 			If the string contains a check symbol but the check symbol is not correct, an IOException is thrown.
   * @param withCheckChar a boolean indicating that the last character is a check sum character
   * @return a long to represent the number encoded by the Crowford Base32 String.
   * @throws IllegalArgumentException when the given string is not a valid Crowford Base32 String
   * @throws IOException when the check symbol doesn't match the character.
   */
  public static long decode(String string, boolean withCheckChar) throws IOException, IllegalArgumentException{
    //check if string only contains valid characters
    if(string.length()==0){
      throw new IllegalArgumentException("The given string is not a valid Crowford Base32 String. It must not be of zero length.");
    }else if(withCheckChar){
      if(!string.matches("^[0-9a-zA-Z\\-]+[0-9a-zA-Z*~$=]-?$")){
        if(string.length()<2){
          throw new IllegalArgumentException("'"+string+"' not a valid Crowford Base32 String. It must must be at least two characters long, if it is supposed to contain a check symbol.");
        }

        throw new IllegalArgumentException("'"+string+"' is not a valid Crowford Base32 String.");
      }
    }else{
      if(!string.matches("^[0-9a-zA-Z\\-]+$")){
        if(string.matches("^[0-9a-zA-Z\\-]+[0-9a-zA-Z*~$=]-?$")){
          throw new IllegalArgumentException("'"+string+"' is not a valid Crowford Base32 String without check symbol. However it is a valid Crowford Base32 String if the last character is a check symbol. Please adjust the call accordingly to decode it.");
        }
        String illegalSymbols = string.replaceAll("[0-9a-zA-Z\\-]", "");
        throw new IllegalArgumentException("'"+string+"' is not a valid Crowford Base32 String. The following symbols from the string are not allowed: "+illegalSymbols);
      }
    }

    //ignore dashes, according to the specification
    String processedString = string.replaceAll("-", "");

    long result = 0;

    //go through the characters
    for(int i=0; i<processedString.length(); i++){
      if(i==processedString.length()-1 && withCheckChar){
        //if this is the last character and it is the check symbol, use the special check character table to decode it
        long checkCharValue = decodeCheckChar(processedString.charAt(i));
        //check if the check character value is different from the check value of the decoded string, if so throw an exception
        if(result%37 != checkCharValue){
          throw new IOException("Check Symbol for '"+string+"' is not correct.");
        }
      }else{
        //read the current character, decode it its character value
        long charValue = decode(processedString.charAt(i));
        //calculate the new result by multiplying the previous result with 32 and adding the value of the current character
        result = result*32 + charValue;
      }
    }

    return result;
  }

  /**
   * a simple function to indicate whether or not a string is a valid Base32 String
   *
   * @param string the string to be checked
   * @param withCheckChar a boolean to indicate whether the last character of the string is supposed to be a check symbol
   * @return true if it is a valid string, false if the string contains illegal characters or if the check symbol does not match the check sum of the decoded number.
   */
  public static boolean check(String string, boolean withCheckChar){
    try {
      decode(string,withCheckChar);
      return true;
    } catch (IllegalArgumentException | IOException e) {
      return false;
    }
  }


  /**
   * Encodes a non negative long into a Crockford Base32 String.
   * <p>is the same as calling <code>{@link #encode(long, boolean, int, int) encode(number, false, 0, 0)}</code></p>
   *
   * @param number the non negative number to encode. An IllegalArgumentException is thrown when the number is negative.
   * @return a Crockford base32 string which represents the number given as parameter
   * @see #encode(long number, boolean withCheckChar, int length, int blockWidth)
   */
  public static String encode(long number){
    return encode(number, false, 0, 0);
  }

  /**
   * Encodes a non negative long into a Crockford Base32 String.
   * <p>is the same as calling <code>{@link #encode(long, boolean, int, int) encode(number, false, length, 0)}</code></p>
   *
   * @param number the non negative number to encode. An IllegalArgumentException is thrown when the number is negative.
   * @param length the minimum length of the resulting base32 string. If the result is shorter then additional zeros are added to beginning of the result string. This parameter will be ignored if it is smaller then the resulting string, including it being zero or negative.
   * @return a Crockford base32 string which represents the number given as parameter
   * @see #encode(long number, boolean withCheckChar, int length, int blockWidth)
   */
  public static String encode(long number, int length){
    return encode(number, false, length, 0);
  }


  /**
   * Encodes a non negative long into a Crockford Base32 String.
   * <p>is the same as calling <code>{@link #encode(long, boolean, int, int) encode(number, false, length, blockWidth)}</code></p>
   *
   * @param number the non negative number to encode. An IllegalArgumentException is thrown when the number is negative.
   * @param length the minimum length of the resulting base32 string. If the result is shorter then additional zeros are added to beginning of the result string. This parameter will be ignored if it is smaller then the resulting string, including it being zero or negative.
   * @param blockWidth the number of characters after which a dash (-) is introduced for better readability. The block width is ignored if it is zero or negative.
   * @return a Crockford base32 string which represents the number given as parameter
   * @see #encode(long number, boolean withCheckChar, int length, int blockWidth)
   */
  public static String encode(long number, int length, int blockWidth){
    return encode(number, false, length, blockWidth);
  }

  /**
   * Encodes a non negative long into a Crockford Base32 String.
   * <p>is the same as calling <code>{@link #encode(long, boolean, int, int) encode(number, withCheckChar, 0, 0)}</code></p>
   *
   * @param number the non negative number to encode. An IllegalArgumentException is thrown when the number is negative.
   * @param withCheckChar a boolean indicating whether the last character of the encoded string should be a checksum character.
   * @return a Crockford base32 string which represents the number given as parameter
   * @see #encode(long number, boolean withCheckChar, int length, int blockWidth)
   */
  public static String encode(long number, boolean withCheckChar){
    return encode(number, withCheckChar, 0, 0);
  }

  /**
   * Encodes a non negative long into a Crockford Base32 String.
   * <p>is the same as calling <code>{@link #encode(long, boolean, int, int) encode(number, withCheckChar, length, 0)}</code></p>
   *
   * @param number the non negative number to encode. An IllegalArgumentException is thrown when the number is negative.
   * @param withCheckChar a boolean indicating whether the last character of the encoded string should be a checksum character.
   * @param length the minimum length of the resulting base32 string. If the result is shorter then additional zeros are added to beginning of the result string. This parameter will be ignored if it is smaller then the resulting string, including it being zero or negative.
   * @return a Crockford base32 string which represents the number given as parameter
   * @see #encode(long number, boolean withCheckChar, int length, int blockWidth)
   */
  public static String encode(long number, boolean withCheckChar, int length){
    return encode(number, withCheckChar, length, 0);
  }

  /**
   * Decodes a Crowford Base32 String into a number.
   *
   * <p>is the same as calling <code>{@link #decode(String, boolean) decode(string, false)}</code></p>
   * @param string The string to be decoded.
   * 			If the string contains characters that are not allowed within a Crowford Base32 String, an IllegalArgumentException is thrown.
   * 			If the string contains a check symbol but the check symbol is not correct, an IOException is thrown.
   * @return a long to represent the number encoded by the Crowford Base32 String.
   * @throws IllegalArgumentException when the given string is not a valid Crowford Base32 String
   * @throws IOException when the check symbol doesn't match the character.
   */
  public static long decode(String string) throws IllegalArgumentException, IOException{
    return decode(string, false);
  }

  private static long decode(char digit) {
    return switch (digit) {
      case '0', 'O', 'o' -> 0;
      case '1', 'I', 'i', 'L', 'l' -> 1;
      case '2' -> 2;
      case '3' -> 3;
      case '4' -> 4;
      case '5' -> 5;
      case '6' -> 6;
      case '7' -> 7;
      case '8' -> 8;
      case '9' -> 9;
      case 'A', 'a' -> 10;
      case 'B', 'b' -> 11;
      case 'C', 'c' -> 12;
      case 'D', 'd' -> 13;
      case 'E', 'e' -> 14;
      case 'F', 'f' -> 15;
      case 'G', 'g' -> 16;
      case 'H', 'h' -> 17;
      case 'J', 'j' -> 18;
      case 'K', 'k' -> 19;
      case 'M', 'm' -> 20;
      case 'N', 'n' -> 21;
      case 'P', 'p' -> 22;
      case 'Q', 'q' -> 23;
      case 'R', 'r' -> 24;
      case 'S', 's' -> 25;
      case 'T', 't' -> 26;

      //Deviation from the standard: u and U are not specified for the regular Base32 characters (only for the check symbols).
      //However, when they are not in the check symbol they are treated as a misread V instead of causing an exception
      //if a check symbol is decoded it checks the special characters first and then checks this code section, so when there is
      //a U as the check symbol it is recognized as having the value 36 first, before check here and become a 27.
      case 'U', 'u', 'V', 'v' -> 27;
      case 'W', 'w' -> 28;
      case 'X', 'x' -> 29;
      case 'Y', 'y' -> 30;
      case 'Z', 'z' -> 31;
      default -> -1;
    };
  }

  private static long decodeCheckChar(char digit) {
    return switch (digit) {
      case '*' -> 32;
      case '~' -> 33;
      case '$' -> 34;
      case '=' -> 35;
      case 'U', 'u' -> 36;
      default -> decode(digit);
    };
  }
}
