package org.flymine.objectstore.ojb;


/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache ObjectRelationalBridge" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache ObjectRelationalBridge", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

//import java.util.Set;
//import java.util.HashSet;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.text.SimpleDateFormat;

import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryNode;
import org.flymine.objectstore.query.QueryEvaluable;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryExpression;
import org.flymine.objectstore.query.QueryFunction;
import org.flymine.objectstore.query.QueryValue;

import org.apache.ojb.broker.accesslayer.sql.SqlStatement;
import org.apache.ojb.broker.accesslayer.conversions.Boolean2IntFieldConversion;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;

/**
 * Code to generate and sql statement...
 *
 * @author Richard Smith
 * @author Matthew Wakeling
 */


public class FlymineSqlSelectStatement implements SqlStatement
{
    private Query query;
    private ClassDescriptor[] clds;

    /**
     * Constructor requires a FlyMine Query and associated array of ClassDescriptors.
     * Should be a ClassDescriptor for each class in FROM clause of query.
     *
     * @param query a flymine query
     * @param clds ClassDescriptor[] for all classes in FROM clause of Query
     */
    public FlymineSqlSelectStatement(Query query, ClassDescriptor[] clds) {
        this.query = query;
        this.clds = clds;
    }

    private String buildSelectComponent() {
        String retval = "";
        boolean needComma = false;
        List select = query.getSelect();
        Iterator selectIter = select.iterator();
        while (selectIter.hasNext()) {
            QueryNode node = (QueryNode) selectIter.next();
            if (needComma) {
                retval += ", ";
            }
            needComma = true;
            if (node instanceof QueryClass) {
                retval += queryClassToString((QueryClass) node);
            } else {
                retval += queryEvaluableToString((QueryEvaluable) node) + " AS "
                    + query.getAliases().get(node);
            }
        }
        return retval;
    }

    private String queryClassToString(QueryClass node) {
        String retval = "";
        boolean needComma = false;
        // It's a class - find its class descriptor, then iterate through its fields.
        // This QueryClass should be aliased as described by Query.getAliases().
        String alias = (String) query.getAliases().get(node);
        boolean done = false;
        ClassDescriptor cld = null;
        for (int i = 0; (i < clds.length) && (!done); i++) {
            cld = clds[i];
            if (cld.getClassOfObject().equals(node.getType())) {
                done = true;
            }
        }
        if (cld == null) {
            throw (new IllegalArgumentException("Couldn't find class descriptor for class "
                        + node.getType()));
        }
        // Now cld is the ClassDescriptor of the node, and alias is the alias
        FieldDescriptor fields[] = cld.getFieldDescriptions();
        for (int i = 0; i < fields.length; i++) {
            FieldDescriptor field = fields[i];
            if (needComma) {
                retval += ", ";
            }
            needComma = true;
            retval += alias + "." + field.getColumnName() + " AS " + alias
                + field.getColumnName();
        }
        return retval;
    }

    private String queryEvaluableToString(QueryEvaluable node) {
        if (node instanceof QueryField) {
            // It's a field - find its FieldDescriptor by looking at its QueryClass, then its
            // ClassDescriptor.
            QueryField nodeF = (QueryField) node;
            QueryClass nodeClass = nodeF.getQueryClass();
            String classAlias = (String) query.getAliases().get(nodeClass);
            //boolean done = false;
            //ClassDescriptor cld = null;
            //for (int i = 0; (i<clds.length) && (! done) ; i++) {
            //    cld = clds[i];
            //    if (cld.getClassOfObject().equals(nodeClass.getType())) {
            //        done = true;
            //    }
            //}
            //if (cld == null) {
            //    throw (new Exception("Couldn't find class descriptor for class "
            //                + nodeClass.getType()));
            //}

            // Now cld is the ClassDescriptor for the node's class. Now need to find node's
            // FieldDescriptor.
            
            return classAlias + "." + nodeF.getFieldName();
        } else if (node instanceof QueryExpression) {
            QueryExpression nodeE = (QueryExpression) node;
            if (nodeE.getOperation() == QueryExpression.SUBSTRING) {
                QueryEvaluable arg1 = nodeE.getArg1();
                int arg2 = ((Integer) ((QueryValue) nodeE.getArg2()).getValue()).intValue();
                int arg3 = ((Integer) nodeE.getArg3().getValue()).intValue();

                return "Substr(" + queryEvaluableToString(arg1) + ", " + arg2 + ", " + arg3 + ")";
            } else {
                QueryEvaluable arg1 = nodeE.getArg1();
                QueryEvaluable arg2 = nodeE.getArg2();
                String op = null;
                switch (nodeE.getOperation()) {
                    case QueryExpression.ADD:
                        op = " + ";
                        break;
                    case QueryExpression.SUBTRACT:
                        op = " - ";
                        break;
                    case QueryExpression.MULTIPLY:
                        op = " * ";
                        break;
                    case QueryExpression.DIVIDE:
                        op = " / ";
                        break;
                    default:
                        throw (new IllegalArgumentException("Invalid QueryExpression operation: "
                                    + nodeE.getOperation()));
                }
                return "(" + queryEvaluableToString(arg1) + op + queryEvaluableToString(arg2) + ")";
            }
        } else if (node instanceof QueryFunction) {
            QueryFunction nodeF = (QueryFunction) node;
            switch (nodeF.getOperation()) {
                case QueryFunction.COUNT:
                    return "COUNT(*)";
                case QueryFunction.SUM:
                    return "SUM(" + queryEvaluableToString(nodeF.getParam()) + ")";
                case QueryFunction.AVERAGE:
                    return "AVG(" + queryEvaluableToString(nodeF.getParam()) + ")";
                case QueryFunction.MIN:
                    return "MIN(" + queryEvaluableToString(nodeF.getParam()) + ")";
                case QueryFunction.MAX:
                    return "MAX(" + queryEvaluableToString(nodeF.getParam()) + ")";
                default:
                    throw (new IllegalArgumentException("Invalid QueryFunction operation: "
                                + nodeF.getOperation()));
            }
        } else if (node instanceof QueryValue) {
            QueryValue nodeV = (QueryValue) node;
            Object value = nodeV.getValue();
            if (value instanceof Date) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                return "'" + format.format((Date) value) + "'";
            } else if (value instanceof Number) {
                return value.toString();
            } else if (value instanceof String) {
                return "'" + value + "'";
            } else if (value instanceof Boolean) {
                return (new Boolean2IntFieldConversion()).javaToSql(value).toString();
            } else {
                throw (new IllegalArgumentException("Invalid Object in QueryValue: "
                            + value.toString()));
            }
        } else {
            throw (new IllegalArgumentException("Invalid QueryEvaluable: " + node.toString()));
        }
    }

    private String buildFromComponent() {
        String retval = "";
        boolean needComma = false;
        return "nothing";
    }

    /**
     * Return the statement as a string
     *
     * @return sql statement as a string
     */
    public String getStatement() {
        return "SELECT " + buildSelectComponent() + " FROM " + buildFromComponent();
    }

}
