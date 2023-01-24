export type Executor = 
    (args: {
        readonly uri: string,
        readonly method: 'GET' | 'POST' | 'PUT' | 'DELETE',
        readonly body?: any
    }) => Promise<any>
;
