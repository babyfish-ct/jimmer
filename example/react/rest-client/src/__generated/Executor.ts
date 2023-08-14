export type Executor = 
    (args: {
        readonly uri: string,
        readonly method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH',
        readonly body?: any
    }) => Promise<any>
;
