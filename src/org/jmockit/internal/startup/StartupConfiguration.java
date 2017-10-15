package org.jmockit.internal.startup;

import javax.annotation.*;
import java.util.*;
import java.util.regex.*;

final class StartupConfiguration
{
   private static final Pattern COMMA_OR_SPACES = Pattern.compile("\\s*,\\s*|\\s+");

   @Nonnull final Collection<String> fakeClasses;

   StartupConfiguration()
   {
      String commaOrSpaceSeparatedValues = System.getProperty("fakes");
      
      if (commaOrSpaceSeparatedValues == null) {
         fakeClasses = Collections.emptyList();
      }
      else {
         List<String> allValues = Arrays.asList(COMMA_OR_SPACES.split(commaOrSpaceSeparatedValues));
         Set<String> uniqueValues = new HashSet<>(allValues);
         uniqueValues.remove("");
         fakeClasses = uniqueValues;
      }
   }
}
