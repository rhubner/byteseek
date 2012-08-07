package net.domesdaybook.compiler.regex;

import net.domesdaybook.automata.State;
import net.domesdaybook.automata.Transition;
import net.domesdaybook.automata.TransitionFactory;
import net.domesdaybook.automata.base.ByteMatcherTransition;
import net.domesdaybook.compiler.matcher.CompilerUtils;
import net.domesdaybook.matcher.bytes.ByteMatcherFactory;
import net.domesdaybook.matcher.bytes.SetAnalysisByteMatcherFactory;
import net.domesdaybook.parser.ParseException;
import net.domesdaybook.parser.tree.ParseTree;


public final class ParseTreeTransitionFactory<T>
  implements TransitionFactory<T, ParseTree> {

  private final ByteMatcherFactory matcherFactory;
  
  public ParseTreeTransitionFactory() {
    this(new SetAnalysisByteMatcherFactory());
  }
  
  public ParseTreeTransitionFactory(final ByteMatcherFactory matcherFactory) {
    this.matcherFactory = matcherFactory;
  }
  
  public Transition<T> create(final ParseTree source,
                              final boolean invert,
                              final State<T> toState) {
    try {
      switch (source.getParseTreeType()) {
        case BYTE:          return createByteTransition(source, toState);
        case ALL_BITMASK:   return createAllBitmaskTransition(source, toState);
        case ANY_BITMASK:   return createAnyBitmaskTransition(source, toState);
        case ANY:           return createAnyTransition(source, toState);
        case SET:           return createSetTransition(source, toState);
      }
    } catch (final ParseException justReturnNull) {
      //TODO: Should a factory throw an exception?  If so, it involves some
      //      extensive refactoring in the classes that use the factories, as
      //      they currently don't expect to fail at creating transitions.
      //      Maybe they should - there are inputs from which a transition can't be
      //      created, for example, the empty set of bytes.  However, this will also
      //      cascade into exceptions for building other kinds of things.  But I guess,
      //      if it's possible to fail due to the arguments passed in, we can only choose 
      //      a checked exception, or throw a runtime IllegalArgumentException...
    }
    return null;
  }

  
  private Transition<T> createByteTransition(final ParseTree ast, final State<T> toState) throws ParseException {
    return new ByteMatcherTransition(CompilerUtils.createByteMatcher(ast), toState);
  }
  
  private Transition<T> createAllBitmaskTransition(final ParseTree ast, final State<T> toState) throws ParseException {
    return new ByteMatcherTransition(CompilerUtils.createAllBitmaskMatcher(ast), toState);
  }

  private Transition<T> createAnyBitmaskTransition(final ParseTree ast, final State<T> toState) throws ParseException {
   return new ByteMatcherTransition(CompilerUtils.createAnyBitmaskMatcher(ast), toState);
  }
  
  private Transition<T> createAnyTransition(final ParseTree ast, final State<T> toState) throws ParseException {
    return new ByteMatcherTransition(CompilerUtils.createAnyMatcher(ast), toState);
  }

  private Transition<T> createSetTransition(final ParseTree ast, final State<T> toState) throws ParseException {
     return new ByteMatcherTransition(CompilerUtils.createMatcherFromSet(ast, matcherFactory), toState);
  }

}