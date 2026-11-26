-- Synthetic records only. Run exclusively inside the temporary release-smoke project.
INSERT INTO users(id,name,email,password,role,virtual_balance,email_verified,account_status)
 VALUES(91001,'Release fixture','release-fixture@example.invalid','not-a-login-password','USER',100000,TRUE,'ACTIVE');
INSERT INTO stocks(id,symbol,instrument_key,exchange,company_name,current_price) VALUES(91001,'FIXTURE','NSE_EQ|RELEASE_FIXTURE','NSE','Release fixture',125);
INSERT INTO holdings(user_id,stock_id,quantity) VALUES(91001,91001,2);
INSERT INTO orders(user_id,stock_id,order_type,status,quantity,price,total_amount,source) VALUES(91001,91001,'BUY','COMPLETED',2,100,200,'EQUITY');
INSERT INTO auth_sessions(id,user_id,expires_at,created_at,last_activity_at,factor_completed) VALUES('synthetic-session',91001,DATE_ADD(NOW(),INTERVAL 1 HOUR),NOW(),NOW(),TRUE);
INSERT INTO ipo_applications(user_id,ipo_id,name,status,reserved_amount) VALUES(91001,'release-fixture-ipo','Release fixture IPO','PENDING',1500);
INSERT INTO equity_pending_orders(id,user_id,request_id,stock_id,units,side,order_kind,trigger_price,reserved_cash,status,created_at,valid_date) VALUES('release-equity',91001,'release-equity',91001,1,'BUY','LIMIT',100,100,'PENDING',NOW(),CURRENT_DATE);
INSERT INTO option_positions(user_id,instrument_key,symbol,expiry,strike_price,option_type,lot_size,units,cost_amount) VALUES(91001,'NSE_FO|RELEASE_FIXTURE','Fixture CE',DATE_ADD(CURRENT_DATE,INTERVAL 30 DAY),25000,'CE',75,75,7500);
INSERT INTO option_orders(id,user_id,request_id,instrument_key,symbol,expiry,strike_price,option_type,lot_size,units,side,order_kind,trigger_price,reserved_cash,status,created_at) VALUES('release-option',91001,'release-option','NSE_FO|RELEASE_FIXTURE','Fixture CE',DATE_ADD(CURRENT_DATE,INTERVAL 30 DAY),25000,'CE',75,75,'BUY','LIMIT',100,7500,'PENDING',NOW());
INSERT INTO paper_tickets(id,user_id,request_id,stock_id,instrument_key,side,product,variety,order_kind,validity,quantity,legs,lot_size,limit_price,ceiling_price,reserved_cash,status,policy_version,expires_at,created_at) VALUES('release-paper',91001,'release-paper-fixture',91001,'NSE_EQ|RELEASE_FIXTURE','BUY','CASH','ICEBERG','LIMIT','DAY',10,2,1,100,100,1000.30,'PENDING','synthetic',DATE_ADD(NOW(),INTERVAL 1 DAY),NOW());
INSERT INTO paper_financing(id,user_id,stock_id,product,quantity,cost_amount,principal,interest,interest_rate,last_price,mark_at,created_at,last_accrued_date) VALUES('release-finance',91001,91001,'MTF',10,1000,500,0.16,0.12,100,NOW(),NOW(),CURRENT_DATE);
INSERT INTO paper_finance_events(event_key,position_id,user_id,event_type,principal_delta,amount,occurred_at) VALUES('release-borrow','release-finance',91001,'BORROW',500,500,NOW());

INSERT INTO paper_intraday_positions(id,user_id,stock_id,direction,quantity,entry_price,collateral,realized_pnl,last_price,mark_at,trade_date,created_at) VALUES('release-intraday',91001,91001,'SHORT',6,100,300,40,90,NOW(),CURRENT_DATE,NOW());
INSERT INTO paper_intraday_events(event_key,user_id,position_id,event_type,units,collateral_delta,realized_pnl,fee,occurred_at) VALUES('release-intraday-open',91001,'release-intraday','OPEN',10,500,0,0.30,NOW());
INSERT INTO paper_intraday_events(event_key,user_id,position_id,event_type,units,collateral_delta,realized_pnl,fee,occurred_at) VALUES('release-intraday-close',91001,'release-intraday','CLOSE',4,-200,40,0.11,NOW());

-- Separate T+1 account: 90 settled + 10 reserved + 20 reusable + 30 restricted.
-- No invented calendar is activated; missing coverage keeps all obligations pending.
INSERT INTO users(id,name,email,password,role,virtual_balance,email_verified,account_status) VALUES(91002,'Settlement restore fixture','settlement-restore@example.invalid','not-a-login-password','USER',120,TRUE,'ACTIVE');
INSERT INTO holdings(user_id,stock_id,quantity) VALUES(91002,91001,3);
INSERT INTO settlement_accounts(user_id,opening_cash,settled_cash,initialized_at) VALUES(91002,100,90,NOW());
INSERT INTO settlement_obligations(id,user_id,source_key,exchange_code,segment_code,trade_date,obligation_kind,credit_policy,amount,remaining_credit,status,created_at) VALUES
 ('restore-t1-reusable',91002,'restore-reusable','NSE','EQUITY','2099-01-01','CASH_CREDIT','REUSABLE',20,20,'PENDING',NOW()),
 ('restore-t1-restricted',91002,'restore-restricted','NSE','EQUITY','2099-01-01','CASH_CREDIT','RESTRICTED',30,30,'PENDING',NOW()),
 ('restore-t1-delivery',91002,'restore-delivery','NSE','EQUITY','2099-01-01','EQUITY_DELIVERY','NONE',0,0,'PENDING',NOW());
INSERT INTO settlement_lots(id,user_id,stock_id,source_key,obligation_id,initial_units,remaining_units,settled,cost_amount,created_at) VALUES('restore-t1-lot',91002,91001,'restore-acquire','restore-t1-delivery',5,3,FALSE,500,NOW());
INSERT INTO settlement_allocations(id,user_id,reference_kind,reference_id,source_key,amount,remaining_amount,status,created_at) VALUES('restore-t1-allocation',91002,'ORDER','restore-order','SETTLED',15,10,'RESERVED',NOW());
INSERT INTO settlement_dependencies(user_id,credit_obligation_id,delivery_obligation_id) VALUES(91002,'restore-t1-restricted','restore-t1-delivery');
INSERT INTO settlement_journal(user_id,event_key,action,fingerprint,amount,occurred_at) VALUES(91002,'restore-disposal','DISPOSE',REPEAT('c',64),2,NOW());
INSERT INTO settlement_lot_events(user_id,event_key,lot_id,action,units,settled_at_event) VALUES(91002,'restore-disposal','restore-t1-lot','DISPOSE',2,FALSE);
INSERT INTO settlement_allocation_events(user_id,event_key,allocation_id,action,amount) VALUES(91002,'restore-release','restore-t1-allocation','RELEASE',5);
INSERT INTO settlement_checks(obligation_id,user_id,last_checked_at) VALUES('restore-t1-reusable',91002,NOW()),('restore-t1-restricted',91002,NOW()),('restore-t1-delivery',91002,NOW());
