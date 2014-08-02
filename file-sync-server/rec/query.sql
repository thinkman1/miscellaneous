SELECT TXN_SOURCE,
       CASE WHEN TXN_SOURCE = 1 THEN 'CheckDeposit'
            WHEN TXN_SOURCE = 2 THEN 'CashDeposit'
            WHEN TXN_SOURCE = 3 THEN 'CheckRescan'
            WHEN TXN_SOURCE = 4 THEN 'CheckCashing'
            WHEN TXN_SOURCE = -1 THEN 'Any'
       END AS TXN_SOURCE_DESC,
       TXN_STATUS,
       CASE WHEN TXN_STATUS = 1 THEN 'Initiated'
            WHEN TXN_STATUS = 2 THEN 'TranInProcess'
            WHEN TXN_STATUS = 3 THEN 'Completed'
            WHEN TXN_STATUS = 4 THEN 'ItemOnEscrow'
            WHEN TXN_STATUS = -1 THEN 'Any'
       END AS TXN_STATUS_DESC,
       TXN_REQUEST_STATUS,
       CASE WHEN TXN_REQUEST_STATUS = 1 THEN 'NotRequested'
            WHEN TXN_REQUEST_STATUS = 2 THEN 'Pending'
            WHEN TXN_REQUEST_STATUS = 3 THEN 'Approved'
            WHEN TXN_REQUEST_STATUS = 4 THEN 'Declined'
            WHEN TXN_REQUEST_STATUS = -1 THEN 'Any'
       END AS TXN_REQUEST_STATUS_DESC,
       TXN_RETURN_STATUS,
       CASE WHEN TXN_RETURN_STATUS = 1 THEN 'NoReturns'
            WHEN TXN_RETURN_STATUS = 2 THEN 'Pending'
            WHEN TXN_RETURN_STATUS = 3 THEN 'Returned'
            WHEN TXN_RETURN_STATUS = 4 THEN 'Captured'
            WHEN TXN_RETURN_STATUS = -1 THEN 'Any'
       END AS TXN_RETURN_STATUS_DESC,
       HANDLER
FROM DEPOSIT_HANDLER;