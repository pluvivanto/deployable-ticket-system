module "db" {
  source  = "terraform-aws-modules/rds/aws"
  version = "~> 6.0"

  identifier = "${var.project_name}-db"

  engine            = "postgres"
  engine_version    = "14"
  instance_class    = "db.t3.micro"
  allocated_storage = 20

  db_name  = var.project_name
  username = "db_user"
  port     = 5432

  manage_master_user_password = true

  vpc_security_group_ids = [aws_security_group.data_sg.id]
  create_db_subnet_group = true
  subnet_ids             = module.vpc.private_subnets

  backup_retention_period = 0
  skip_final_snapshot     = true
  deletion_protection     = false

  family = "postgres14"
}

resource "aws_elasticache_subnet_group" "default" {
  name       = "${var.project_name}-redis-subnet-group"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_elasticache_cluster" "redis" {
  cluster_id           = "${var.project_name}-redis"
  engine               = "redis"
  node_type            = "cache.t3.micro"
  num_cache_nodes      = 1
  parameter_group_name = "default.redis7"
  engine_version       = "7.0"
  port                 = 6379
  subnet_group_name    = aws_elasticache_subnet_group.default.name
  security_group_ids   = [aws_security_group.data_sg.id]
}