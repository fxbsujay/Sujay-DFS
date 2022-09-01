
export interface StorageModel {
    host: string
    port: number
    httpPort: number
    status: number
    storedSize: bigint
    filePath: string
}

export class TrackerModel {
    host: string = ''
    port: number | string = ''
    httpPort: number | string = ''
    baseDir: string = ''
    logBaseDir: string = ''
    totalStoredSize: bigint | string = ''
    fileCount: bigint | string = ''
}


export class FileTreeModel {
    index: number = 0
    path: string = ''
    type: number | string = ''
    children: Array<FileTreeModel> = []
    attr: Map<string,string> = new Map<string, string>()
    fileSize: bigint | string = ''
}
