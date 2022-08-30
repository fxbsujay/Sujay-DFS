
export interface StorageModel {
    host: string
    status: number
    storedSize: bigint
    filePath: string
}

export interface FileTreeModel {
    path: string
    type: number
    children: FileTreeModel
    attr: Map<string,string>
    fileSize: bigint
}
