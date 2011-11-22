package org.thobe.playthings.aether;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;

public class NeoRepo<T>
{

    private final GraphDatabaseService graphdb;
    private final Class<T> type;
    private final Index<Node> index;

    public NeoRepo( GraphDatabaseService graphdb, Class<T> type )
    {
        this.graphdb = graphdb;
        this.type = type;
        this.index = graphdb.index().forNodes( type.getSimpleName() );
    }

    public T getOrCreate( String key, String value )
    {
        Node node = index.get( key, value ).getSingle();
        if ( node == null )
        {
            Transaction tx = graphdb.beginTx();
            try
            {
                node = index.get( key, value ).getSingle();
                if ( node == null )
                {
                    node = graphdb.createNode();
                    node.setProperty( key, value );
                    index.add( node, key, value );
                }
                tx.success();
            }
            finally
            {
                tx.finish();
            }
        }
        return type.cast( Proxy.newProxyInstance( type.getClassLoader(), new Class<?>[] { type },
                new NodeHandler( node ) ) );
    }

    private class NodeHandler implements InvocationHandler
    {
        private final Node node;

        NodeHandler( Node node )
        {
            this.node = node;
        }

        public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
        {
            if ( args == null || args.length == 0 )
            {
                if ( "hashCode".equals( method.getName() ) )
                {
                    return node.hashCode();
                }
                else if ( method.getName().startsWith( "get" ) )
                {
                    return node.getProperty( method.getName().substring( 3 ) );
                }
            }
            else if ( args.length == 1 )
            {
                if ( method.getParameterTypes()[0] == Object.class && "equals".equals( method.getName() ) )
                {
                    if ( proxy == args[0] ) return true;
                    if ( null == args[0] ) return false;
                    if ( Proxy.isProxyClass( args[0].getClass() ) )
                    {
                        InvocationHandler handler = Proxy.getInvocationHandler( args[0] );
                        if ( handler instanceof NeoRepo<?>.NodeHandler )
                            return ( (NodeHandler) handler ).node.equals( node );
                    }
                    return false;
                }
                else if ( method.getName().startsWith( "set" ) )
                {
                    node.setProperty( method.getName().substring( 3 ), args[0] );
                    return null;
                }
            }
            throw new UnsupportedOperationException();
        }
    }
}
