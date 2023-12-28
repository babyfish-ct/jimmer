export type Executor = 
    (args: {
        readonly uri: string,
        readonly method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH',
        readonly headers?: {readonly [key:string]: string},
        readonly body?: any,
    }) => Promise<any>
;
