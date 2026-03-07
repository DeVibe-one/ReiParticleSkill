// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.math;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight mathematical expression evaluator.
 * <p>
 * Supports:
 * <ul>
 *   <li>Arithmetic: {@code + - * / % ^}</li>
 *   <li>Unary minus</li>
 *   <li>Parentheses</li>
 *   <li>Variables (case-sensitive): set via {@link #with(String, double)}</li>
 *   <li>Constants: {@code PI}, {@code E}</li>
 *   <li>Functions: {@code sin cos tan asin acos atan sqrt abs floor ceil log exp pow min max}</li>
 * </ul>
 * <p>
 * Thread-safety: instances are <b>not</b> thread-safe. Create one per thread
 * or synchronise externally.
 * <p>
 * Usage:
 * <pre>{@code
 * double result = new ExpressionEvaluator("sin(t * PI / 180)")
 *         .with("t", 90)
 *         .evaluate();
 * }</pre>
 */
public final class ExpressionEvaluator {

    private final String expression;
    private final Map<String, Double> variables = new HashMap<>();
    private int pos;
    private int ch;

    public ExpressionEvaluator(String expression) {
        this.expression = expression;
        variables.put("PI", Math.PI);
        variables.put("E", Math.E);
    }

    public ExpressionEvaluator with(String name, double value) {
        variables.put(name, value);
        return this;
    }

    public ExpressionEvaluator and(String name, double value) {
        return with(name, value);
    }

    public double evaluate() {
        pos = -1;
        advance();
        double result = parseExpression();
        if (pos < expression.length()) {
            throw new RuntimeException("Unexpected character: " + (char) ch);
        }
        return result;
    }

    private void advance() {
        ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
    }

    private boolean consume(int expected) {
        skipWhitespace();
        if (ch == expected) {
            advance();
            return true;
        }
        return false;
    }

    private void skipWhitespace() {
        while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
            advance();
        }
    }

    // Grammar: expression = term (('+' | '-') term)*
    private double parseExpression() {
        double x = parseTerm();
        for (;;) {
            skipWhitespace();
            if (consume('+')) {
                x += parseTerm();
            } else if (consume('-')) {
                x -= parseTerm();
            } else {
                return x;
            }
        }
    }

    // term = power (('*' | '/' | '%') power)*
    private double parseTerm() {
        double x = parsePower();
        for (;;) {
            skipWhitespace();
            if (consume('*')) {
                x *= parsePower();
            } else if (consume('/')) {
                double d = parsePower();
                x = (d == 0) ? 0 : x / d;
            } else if (consume('%')) {
                double d = parsePower();
                x = (d == 0) ? 0 : x % d;
            } else {
                return x;
            }
        }
    }

    // power = unary ('^' unary)*   (right-associative)
    private double parsePower() {
        double x = parseUnary();
        skipWhitespace();
        if (consume('^')) {
            x = Math.pow(x, parsePower());
        }
        return x;
    }

    // unary = ('+' | '-')? atom
    private double parseUnary() {
        skipWhitespace();
        if (consume('+')) return parseUnary();
        if (consume('-')) return -parseUnary();
        return parseAtom();
    }

    // atom = number | '(' expression ')' | function '(' args ')' | variable
    private double parseAtom() {
        skipWhitespace();
        // Parenthesised expression
        if (consume('(')) {
            double x = parseExpression();
            consume(')');
            return x;
        }

        // Number literal
        if ((ch >= '0' && ch <= '9') || ch == '.') {
            return parseNumber();
        }

        // Identifier: function or variable
        if (isIdentStart(ch)) {
            return parseIdentifier();
        }

        throw new RuntimeException("Unexpected character: " + (char) ch + " at position " + pos);
    }

    private double parseNumber() {
        int start = pos;
        while ((ch >= '0' && ch <= '9') || ch == '.') advance();
        // Handle scientific notation (e.g. 1e-3, 2.5E+10)
        if (ch == 'e' || ch == 'E') {
            advance();
            if (ch == '+' || ch == '-') advance();
            while (ch >= '0' && ch <= '9') advance();
        }
        return Double.parseDouble(expression.substring(start, pos));
    }

    private double parseIdentifier() {
        int start = pos;
        while (isIdentPart(ch)) advance();
        String name = expression.substring(start, pos);
        skipWhitespace();

        // Function call
        if (ch == '(') {
            advance(); // consume '('
            double arg1 = parseExpression();
            if (consume(',')) {
                double arg2 = parseExpression();
                consume(')');
                return evalFunction2(name, arg1, arg2);
            }
            consume(')');
            return evalFunction1(name, arg1);
        }

        // Variable / constant
        Double val = variables.get(name);
        if (val != null) return val;

        throw new RuntimeException("Unknown variable: " + name);
    }

    private static double evalFunction1(String name, double arg) {
        return switch (name) {
            case "sin" -> Math.sin(arg);
            case "cos" -> Math.cos(arg);
            case "tan" -> Math.tan(arg);
            case "asin" -> Math.asin(arg);
            case "acos" -> Math.acos(arg);
            case "atan" -> Math.atan(arg);
            case "sqrt" -> Math.sqrt(arg);
            case "abs" -> Math.abs(arg);
            case "floor" -> Math.floor(arg);
            case "ceil" -> Math.ceil(arg);
            case "round" -> Math.round(arg);
            case "log" -> Math.log(arg);
            case "log10" -> Math.log10(arg);
            case "exp" -> Math.exp(arg);
            case "sign", "signum" -> Math.signum(arg);
            case "toRadians", "radians", "rad" -> Math.toRadians(arg);
            case "toDegrees", "degrees", "deg" -> Math.toDegrees(arg);
            default -> throw new RuntimeException("Unknown function: " + name);
        };
    }

    private static double evalFunction2(String name, double a, double b) {
        return switch (name) {
            case "pow" -> Math.pow(a, b);
            case "min" -> Math.min(a, b);
            case "max" -> Math.max(a, b);
            case "atan2" -> Math.atan2(a, b);
            default -> throw new RuntimeException("Unknown function: " + name);
        };
    }

    private static boolean isIdentStart(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean isIdentPart(int c) {
        return isIdentStart(c) || (c >= '0' && c <= '9');
    }
}
