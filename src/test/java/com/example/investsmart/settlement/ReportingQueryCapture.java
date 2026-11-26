package com.example.investsmart.settlement;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/** Synthetic test instrumentation only. Captures executed prepared SELECTs, never connection credentials. */
final class ReportingQueryCapture extends DelegatingDataSource {
    record Query(String sql,List<Object> parameters) {}
    private final List<Query> queries=new ArrayList<>();
    ReportingQueryCapture(DataSource source){super(source);}
    synchronized List<Query> snapshot(){return List.copyOf(queries);}
    synchronized void clear(){queries.clear();}
    @Override public Connection getConnection()throws SQLException{return wrap(super.getConnection());}
    @Override public Connection getConnection(String user,String password)throws SQLException{return wrap(super.getConnection(user,password));}
    private Connection wrap(Connection connection){
        return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->{
            Object value=invoke(method,connection,args);
            if(method.getName().equals("prepareStatement")&&args[0] instanceof String sql&&value instanceof PreparedStatement statement)return statement(statement,sql);
            return value;
        });
    }
    private PreparedStatement statement(PreparedStatement statement,String sql){
        var parameters=new TreeMap<Integer,Object>();
        return (PreparedStatement)Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(proxy,method,args)->{
            Object value=invoke(method,statement,args);
            if(method.getName().equals("clearParameters"))parameters.clear();
            else if(method.getName().startsWith("set")&&args!=null&&args.length>=2&&args[0] instanceof Integer index){
                Object parameter=method.getName().equals("setNull")?null:args[1];
                if(parameter instanceof Timestamp timestamp)parameter=timestamp.toInstant();
                else if(parameter instanceof java.sql.Date date)parameter=date.toLocalDate();
                parameters.put(index,parameter);
            }
            if(method.getName().equals("executeQuery")&&sql.stripLeading().toUpperCase(Locale.ROOT).startsWith("SELECT")){
                var values=new ArrayList<Object>();for(int i=1;i<=parameters.size();i++)values.add(parameters.get(i));
                synchronized(this){queries.add(new Query(sql,Collections.unmodifiableList(values)));}
            }
            return value;
        });
    }
    private static Object invoke(Method method,Object target,Object[] args)throws Throwable{
        try{return method.invoke(target,args);}catch(InvocationTargetException failed){throw failed.getCause();}
    }
}
